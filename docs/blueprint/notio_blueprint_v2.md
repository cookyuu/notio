# Notio — 아키텍처 재설계 Blueprint v2.1

> **버전**: v2.1
> **작성일**: 2026-05-12
> **이전 버전**: v2.0 (`notio_blueprint_v2.md` — 2026-05-12)
> **범위**: 웹 대시보드 + 채팅 플랫폼 알림 전달 + LLM 요약 파이프라인 재설계

---

## 목차

1. [재설계 배경 및 방향](#1-재설계-배경-및-방향)
2. [아키텍처 변화 (v2.0 → v2.1)](#2-아키텍처-변화-v20--v21)
3. [기술 스택 변경사항](#3-기술-스택-변경사항)
4. [Backend 설계 — `channel/` 모듈](#4-backend-설계--channel-모듈)
5. [알림 전달 플랫폼별 명세](#5-알림-전달-플랫폼별-명세)
6. [라우팅 규칙 엔진 + Digest 모드](#6-라우팅-규칙-엔진--digest-모드)
7. [LLM 전략 — 알림 요약 파이프라인](#7-llm-전략--알림-요약-파이프라인)
8. [DB 스키마 변경 (V12–V14)](#8-db-스키마-변경-v12v14)
9. [Frontend — Flutter Web 대시보드](#9-frontend--flutter-web-대시보드)
10. [인프라 및 배포](#10-인프라-및-배포)
11. [Connection vs Channel 개념 구분](#11-connection-vs-channel-개념-구분)
12. [보안 설계](#12-보안-설계)
13. [운영 및 모니터링](#13-운영-및-모니터링)
14. [Phase 로드맵](#14-phase-로드맵)
15. [기술 의사결정 기록 (ADR)](#15-기술-의사결정-기록-adr)

---

## 1. 재설계 배경 및 방향

### 1.1 v2.0 설계의 한계

v2.0은 FCM 모바일 푸시를 제거하고 Slack/Telegram/Discord 채널 전달로 전환하는 방향을 정의했다.
그러나 LLM+RAG의 역할을 "인터랙티브 AI 채팅"과 "채널 알림 전달"에 동시에 사용하는 구조는
아래 문제를 내포했다:

| 문제 | 세부 내용 |
|------|-----------|
| 불필요한 채팅 인터페이스 | Codex/Claude Code 알림은 이미 AI가 생성한 텍스트 — 다시 AI에게 묻는 구조가 불합리 |
| 인터랙티브 채팅 복잡도 | SSE 스트리밍, ChatMessage 영속성, RAG+대화이력 관리 — 핵심 가치 대비 비용 과다 |
| 알림 도착 즉시 채널 전달 | 짧은 시간 내 다수 알림 → 채널 메시지 폭주. 사용자가 제어할 방법 없음 |

### 1.2 재설계 핵심 방향 (v2.1)

```
v2.0: LLM+RAG = 인터랙티브 채팅 + 알림 요약
v2.1: LLM+RAG = 알림 요약 파이프라인 전용
      + 사용자 설정 가능한 전달 타이밍 (즉시 / 묶음 N분)
```

**변경 요약:**

| 구성요소 | v2.0 | v2.1 |
|---------|------|------|
| AI 채팅 화면 | LLM과 인터랙티브 채팅 | 채널 전달 메시지 피드 (Delivery Feed) |
| LLM+RAG 역할 | 채팅 응답 + 알림 요약 | **알림 요약 파이프라인 전용** |
| 채널 전달 타이밍 | 즉시 전송만 | 즉시 / 묶음(10/20/30분) — RoutingRule 단위 설정 |
| chat_messages 테이블 | 대화 이력 저장 | 폐기 (90일 후 DROP) |

### 1.3 유지되는 핵심 가치

- Webhook 수신 (Claude Code, Codex, Slack, GitHub, Gmail) — **변경 없음**
- Slack Bot / Telegram Bot / Discord Webhook 채널 전달 — **변경 없음**
- JWT 인증, 알림 저장/조회/필터 — **변경 없음**
- 관측성 (Prometheus, 구조화 로그, Zipkin) — **변경 없음**
- LLM+RAG 인프라 (Ollama, pgvector, EmbeddingProvider) — **역할만 변경, 코드 구조 유지**

---

## 2. 아키텍처 변화 (v2.0 → v2.1)

### 2.1 제거 항목

| 패키지/클래스 | 이유 |
|-------------|------|
| `chat/` 패키지 전체 | AI 인터랙티브 채팅 기능 제거 |
| `ChatService`, `DailySummaryService` | 채팅 + 일일요약 서비스 불필요 |
| `ChatController` | `/api/v1/chat` 엔드포인트 제거 |
| `ChatMessage`, `ChatMessageRole` | 채팅 이력 엔티티 불필요 |
| `ChatMessageRepository` | 동상 |
| `ChatPromptContext`, `ChatTimeRangeExtractor`, `ChatMetrics` | 채팅 전용 유틸리티 |
| `PushService`, `DeviceService`, `Device` | FCM 불필요 (v2.0에서 이미 예정) |
| `FirebaseConfig` | Firebase Admin SDK 제거 |

### 2.2 TO-BE 아키텍처 (v2.1)

```
[Web Browser]
  Flutter Web · SSE 실시간 업데이트
         │ HTTPS + JWT
         ▼
[Nginx (80/443)]
  /          ──► Flutter Web static files
  /api/      ──► Spring Boot 8080
  /actuator/ ──► 내부망 전용 (차단)
         │
[Spring Boot 8080]
  webhook/ ◄─── Claude Code / Codex / Slack / GitHub / Gmail
  notification/
    └── NotificationSummaryService ─► LLM+RAG 요약 파이프라인
  channel/ ──────────────────────► Slack Bot API
  (IMMEDIATE/DIGEST 분기)  ──────► Telegram Bot API
                           ──────► Discord Webhook URL
  auth/ · todo/ · analytics/ · connection/
         │
  [PostgreSQL 16 + pgvector]
  [Redis 7]
  [Ollama llama3.2:3b] (선택적 기동, profiles)
  [Let's Encrypt SSL]
```

### 2.3 알림 처리 흐름 변화

```
v2.0:
  Webhook ──► NotificationService ──► [async] ChannelRouter.route()
                                   ──► [async] PushService.sendPush() (FCM, 폐기)

v2.1:
  Webhook ──► NotificationService.save()
                    │
                    ├─ [async A] NotificationEmbeddingService.embed()
                    │            → pgvector 임베딩 저장 (RAG용)
                    │
                    └─ [async B] NotificationSummaryService.summarize()
                                  │
                                  ├─ shouldSummarize()
                                  │   → notio.ai.summarize-sources 설정 확인
                                  │   → 기본: CLAUDE, CODEX 소스만 요약
                                  │
                                  ├─ RagRetriever.retrieve()
                                  │   → 유사 과거 알림 컨텍스트 추출 (pgvector)
                                  │
                                  ├─ LlmProvider.chat(summaryPrompt)
                                  │   → ai_summary 생성
                                  │
                                  └─ UPDATE notifications.ai_summary
                                               │
                                               ▼
                                  ChannelRouter.route(notification)
                                    │
                                    RoutingRule 평가 (source/priority/delivery_mode)
                                    │
                          ┌─────── IMMEDIATE ──────────┐
                          ▼                            ▼
                    ChannelMessage                DigestChannelRouter.queue()
                    .body = COALESCE(              → DIGEST_PENDING 큐잉
                      ai_summary, body)            → next_delivery_at = now + interval
                          │
                    SlackProvider / TelegramProvider / DiscordProvider
                          │
                    ChannelDeliveryLog(SUCCESS)

  ※ NotificationDigestScheduler (@Scheduled, 1분 주기):
    → DIGEST_PENDING AND next_delivery_at <= now 그룹 처리
    → LlmProvider.chat(buildDigestSummaryPrompt(notifications))
    → 채널에 1개 묶음 메시지 전송
    → 그룹 내 모든 ChannelDeliveryLog → SUCCESS

  ※ Branch A(임베딩) / Branch B(요약+라우팅) 는 완전 병렬 실행
  ※ LLM 실패/타임아웃 시 ai_summary=null → body 원본 사용 (graceful degradation)
```

---

## 3. 기술 스택 변경사항

### 3.1 제거 항목

| 항목 | 이유 |
|------|------|
| Firebase Admin SDK (`firebase-admin:9.4.2`) | FCM 불필요 |
| `firebase_core`, `firebase_messaging` (Flutter) | FCM 불필요 |
| `flutter_local_notifications` | 웹 미지원 네이티브 전용 |
| `sqlite3_flutter_libs` | Flutter Web 빌드 블로커 |

### 3.2 추가 항목

| 항목 | 용도 |
|------|------|
| `spring-ai-anthropic-spring-boot-starter` | Claude API LLM Provider (선택) |
| Nginx (Docker) | 리버스 프록시 + Flutter Web 서빙 |
| Certbot (Docker) | Let's Encrypt 자동 갱신 |

### 3.3 유지 항목 (전체 목록)

**Backend:**
- Java 25 / Spring Boot 4.0.0 / Gradle 9.0
- Spring Data JPA 7.1.x, Spring Security, JWT (jjwt 0.12.6)
- PostgreSQL 16 + pgvector, Redis 7, Flyway 11.1.0
- Spring AI (Ollama), Ollama llama3.2:3b + nomic-embed-text
- Testcontainers, JUnit 5, Mockito

**Frontend:**
- Dart 3.6.x / Flutter 3.27.x
- hooks_riverpod 2.5.3, go_router 14.6.1, Dio 5.4.3
- Drift 2.18.0 (SQLite, web 연결 이미 분기됨)
- flutter_secure_storage 9.2.2, fl_chart 0.68.0

---

## 4. Backend 설계 — `channel/` 모듈

### 4.1 패키지 구조

```
com/notio/
├── ai/                                         (유지, 역할만 변경)
│   ├── embedding/
│   │   ├── EmbeddingProvider.java
│   │   └── OllamaEmbeddingProvider.java
│   ├── llm/
│   │   ├── LlmProvider.java
│   │   ├── OllamaLlmProvider.java
│   │   └── AnthropicLlmProvider.java           (신규, @ConditionalOnProperty)
│   ├── metrics/
│   │   └── LlmMetrics.java                     (신규, ChatMetrics 대체)
│   ├── prompt/
│   │   ├── LlmPrompt.java
│   │   └── PromptBuilder.java                  (수정: chat/daily 제거, summary/digest 추가)
│   └── rag/
│       ├── PgvectorRagRetriever.java            (수정: ChatMetrics → LlmMetrics)
│       ├── RagDocument.java
│       ├── RagRetriever.java
│       └── TimeRange.java
│
├── notification/
│   ├── domain/
│   │   └── Notification.java                   (수정: aiSummary 필드 추가)
│   ├── service/
│   │   ├── NotificationService.java            (수정: 요약+라우팅 비동기 오케스트레이션)
│   │   └── NotificationSummaryService.java     (신규)
│   ├── metrics/
│   │   └── NotificationFlowMetrics.java        (수정: summarization 메트릭 추가)
│   └── dto/
│       └── NotificationDetailResponse.java     (수정: aiSummary 필드 추가)
│
├── channel/
│   ├── domain/
│   │   ├── NotificationChannel.java
│   │   ├── ChannelType.java              # SLACK | TELEGRAM | DISCORD
│   │   ├── ChannelStatus.java            # ACTIVE | PAUSED | ERROR
│   │   ├── DeliveryStatus.java           # PENDING | SUCCESS | FAILED | RETRY | DEAD | DIGEST_PENDING
│   │   ├── DeliveryMode.java             # IMMEDIATE | DIGEST  (신규)
│   │   ├── RoutingRule.java              (수정: deliveryMode, digestIntervalMin 추가)
│   │   ├── RoutingCondition.java
│   │   └── ChannelDeliveryLog.java
│   ├── provider/
│   │   ├── NotificationChannelProvider.java
│   │   ├── ChannelMessage.java
│   │   ├── ChannelDeliveryResult.java
│   │   ├── ChannelValidationResult.java
│   │   ├── slack/
│   │   │   ├── SlackChannelProvider.java
│   │   │   └── SlackBlockKitFormatter.java
│   │   ├── telegram/
│   │   │   ├── TelegramChannelProvider.java
│   │   │   └── TelegramMarkdownFormatter.java
│   │   └── discord/
│   │       ├── DiscordChannelProvider.java
│   │       └── DiscordEmbedFormatter.java
│   ├── routing/
│   │   ├── ChannelRouter.java               # IMMEDIATE 경로 전담
│   │   ├── DigestChannelRouter.java         # DIGEST 경로 전담 (신규)
│   │   ├── RoutingRuleEvaluator.java
│   │   └── ChannelProviderRegistry.java
│   ├── retry/
│   │   ├── ChannelDeliveryScheduler.java    # 기존 RETRY 백오프 처리
│   │   └── NotificationDigestScheduler.java # 신규: DIGEST 윈도우 만료 처리
│   ├── controller/
│   │   ├── NotificationChannelController.java
│   │   ├── RoutingRuleController.java
│   │   └── DeliveryFeedController.java      # 신규: GET /api/v1/channels/delivery-feed
│   ├── service/
│   │   ├── NotificationChannelService.java
│   │   └── RoutingRuleService.java
│   ├── repository/
│   │   ├── NotificationChannelRepository.java
│   │   ├── RoutingRuleRepository.java
│   │   └── ChannelDeliveryLogRepository.java
│   ├── dto/
│   │   ├── CreateChannelRequest.java
│   │   ├── UpdateChannelRequest.java
│   │   ├── ChannelResponse.java
│   │   ├── CreateRoutingRuleRequest.java    (수정: deliveryMode, digestIntervalMin 추가)
│   │   ├── UpdateRoutingRuleRequest.java    (수정: 동상)
│   │   ├── RoutingRuleResponse.java         (수정: 동상)
│   │   └── DeliveryFeedItem.java            (신규)
│   └── metrics/
│       └── ChannelDeliveryMetrics.java
```

### 4.2 핵심 인터페이스 (OCP)

```java
public interface NotificationChannelProvider {
    ChannelType supports();

    /**
     * 채널에 메시지 전달.
     * 네트워크 예외는 호출자(ChannelRouter)가 처리.
     * 비즈니스 결과(성공/실패/재시도 가능 여부)만 반환.
     */
    ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message);

    /**
     * 채널 자격증명 유효성 검증 (채널 저장 전 호출).
     * 실제 API 호출로 Bot Token/Webhook URL 확인.
     */
    ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier);
}
```

### 4.3 도메인 모델

#### NotificationChannel 엔티티

```
notification_channels 테이블 매핑

필드:
  id                  BIGSERIAL PK
  user_id             BIGINT FK → users
  channel_type        SLACK | TELEGRAM | DISCORD
  display_name        VARCHAR(100)
  credential_encrypted TEXT       — AES-256-GCM
                                    Slack: Bot OAuth Token (xoxb-...)
                                    Telegram: Bot Token (123456:ABC...)
                                    Discord: Webhook URL 전체
  target_identifier   VARCHAR(255) — Slack: channel_id, Telegram: chat_id
                                     Discord: null (Webhook URL에 포함)
  status              ACTIVE | PAUSED | ERROR
  error_count         INT
  last_error          TEXT
  last_delivered_at   TIMESTAMPTZ
  created_at / updated_at / deleted_at
```

#### RoutingRule 엔티티

```
routing_rules 테이블 매핑

필드:
  id                  BIGSERIAL PK
  user_id             BIGINT FK → users
  rule_name           VARCHAR(100)
  priority_order      INT — 낮을수록 먼저 평가 (기본 100)
  conditions          JSONB — {"sources": ["GITHUB", "CLAUDE"], "priorities": ["HIGH", "URGENT"]}
  channel_ids         JSONB — [1, 3, 7]
  stop_on_match       BOOLEAN
  is_enabled          BOOLEAN
  delivery_mode       IMMEDIATE | DIGEST   (기본: IMMEDIATE)
  digest_interval_min INT NULL             (DIGEST 시 10/20/30/60 등)
  created_at / updated_at / deleted_at
```

#### ChannelDeliveryLog 엔티티

```
channel_delivery_logs 테이블 매핑

필드:
  id                  BIGSERIAL PK
  notification_id     BIGINT FK → notifications
  channel_id          BIGINT FK → notification_channels
  status              PENDING | SUCCESS | FAILED | RETRY | DEAD | DIGEST_PENDING
  attempt_count       INT
  last_error          TEXT
  external_message_id VARCHAR(255)  — Slack: ts, Telegram: message_id, Discord: id
  next_retry_at       TIMESTAMPTZ  — RETRY 백오프 / DIGEST 윈도우 만료 시각 공용
  delivered_at        TIMESTAMPTZ
  created_at / updated_at

제약:
  UNIQUE (notification_id, channel_id) WHERE status IN ('PENDING', 'RETRY', 'DIGEST_PENDING')
```

### 4.4 NotificationSummaryService

```java
@Service
@RequiredArgsConstructor
public class NotificationSummaryService {

    private final RagRetriever ragRetriever;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final NotificationRepository notificationRepository;
    private final NotioAiProperties aiProperties;
    private final NotificationFlowMetrics metrics;

    /**
     * 알림을 LLM+RAG로 요약하여 ai_summary 컬럼에 저장.
     * 실패 시 null 반환 — 호출자는 null을 "원본 body 사용"으로 처리.
     */
    @Nullable
    public String summarize(Notification notification) {
        if (!shouldSummarize(notification)) return null;
        Instant start = Instant.now();
        try {
            List<RagDocument> context = ragRetriever.retrieve(
                notification.getUserId(),
                notification.getTitle() + " " + notification.getBody(),
                Optional.empty()
            );
            LlmPrompt prompt = promptBuilder.buildNotificationSummaryPrompt(notification, context);
            String summary = llmProvider.chat(prompt);
            notificationRepository.updateAiSummary(notification.getId(), summary);
            metrics.recordAiSummarization("success", Duration.between(start, Instant.now()));
            return summary;
        } catch (Exception e) {
            log.warn("event=ai_summarization_failed notification_id={}", notification.getId(), e);
            metrics.recordAiSummarization("failure", Duration.between(start, Instant.now()));
            return null;
        }
    }

    private boolean shouldSummarize(Notification n) {
        Set<String> configured = new HashSet<>(aiProperties.summarizeSources());
        return configured.isEmpty() || configured.contains(n.getSource().name());
    }
}
```

### 4.5 ChannelMessage 구성 (ai_summary 우선)

```java
// ChannelRouter.route() 내부
ChannelMessage message = ChannelMessage.builder()
    .notificationId(notification.getId())
    .title(notification.getTitle())
    .body(notification.getAiSummary() != null
          ? notification.getAiSummary()
          : notification.getBody())        // graceful fallback
    .source(notification.getSource())
    .priority(notification.getPriority())
    .externalUrl(notification.getExternalUrl())
    .notifiedAt(notification.getCreatedAt())
    .build();
```

### 4.6 DeliveryFeedController

```
GET /api/v1/channels/delivery-feed

Query params:
  page        int    (default 0)
  size        int    (default 20, max 50)
  channelType string (optional: SLACK | TELEGRAM | DISCORD)

Response: ApiResponse<Page<DeliveryFeedItem>>

DeliveryFeedItem {
  deliveryLogId       Long
  notificationId      Long
  notificationTitle   String
  channelId           Long
  channelType         ChannelType
  channelDisplayName  String
  deliveredContent    String    — COALESCE(n.ai_summary, n.body)
  deliveredAt         Instant
  status              DeliveryStatus
  externalMessageId   String    (nullable)
}
```

### 4.7 NotificationService 변경점

```java
// 변경 전
private final PushService pushService;
// ...
try { pushService.sendPush(saved.getId(), saved.getUserId()); }
catch (Exception e) { log.warn(...); }

// 변경 후
private final NotificationSummaryService notificationSummaryService;
private final ChannelRouter channelRouter;
// ...

// Branch A: 임베딩 (병렬)
CompletableFuture.runAsync(() -> {
    try { notificationEmbeddingService.embedNotification(saved); }
    catch (Exception e) { log.warn("event=embedding_failed", e); }
});

// Branch B: 요약 → 채널 라우팅 (순차, 병렬로 실행)
CompletableFuture.runAsync(() -> {
    try {
        notificationSummaryService.summarize(saved); // ai_summary DB 저장
        channelRouter.route(saved);                   // ai_summary 읽어서 채널 전달
    } catch (Exception e) {
        log.error("event=notification_pipeline_failed notification_id={}", saved.getId(), e);
    }
});
```

---

## 5. 알림 전달 플랫폼별 명세

### 5.1 Slack

**인증 방식:** Bot OAuth Token (`xoxb-...`) + Channel ID (`C0123456789`)

**Bot 설정 절차:**
1. api.slack.com → Create New App → From Scratch
2. OAuth & Permissions → Bot Token Scopes: `chat:write`, `chat:write.public`
3. Install to Workspace → Bot User OAuth Token 복사
4. 채널에 `/invite @botname`

**메시지 포맷 (Block Kit):**
```json
{
  "channel": "C0123456789",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": ":github: *[GitHub] PR Review Required*"
      }
    },
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "코드 리뷰 요청이 들어왔습니다. 변경사항을 확인해주세요."
      }
    },
    {
      "type": "context",
      "elements": [
        {"type": "mrkdwn", "text": "Source: *GITHUB*"},
        {"type": "mrkdwn", "text": "Priority: *HIGH*"},
        {"type": "mrkdwn", "text": "<https://github.com/...|View Original>"}
      ]
    }
  ],
  "attachments": [{"color": "#FF8C00", "fallback": "PR Review Required"}]
}
```

**우선순위별 색상:**
- URGENT: `#FF0000` / HIGH: `#FF8C00` / MEDIUM: `#4A90E2` / LOW: `#9B9B9B`

**전달 API:** `POST https://slack.com/api/chat.postMessage`
**검증 API:** `POST https://slack.com/api/auth.test`

**에러 처리:**
- `ok: false` + `error: "ratelimited"` → retryable=true
- `ok: false` + 기타 에러 → retryable=false
- HTTP 4xx → retryable=false / HTTP 5xx → retryable=true

---

### 5.2 Telegram

**인증 방식:** Bot Token (`123456:ABC-DEF...`) + Chat ID (`-100123456789`)

**Bot 설정 절차:**
1. `@BotFather` 대화 → `/newbot` → Token 발급
2. Bot을 채널/그룹에 추가 (관리자 권한)
3. Chat ID 확인: `https://api.telegram.org/bot{TOKEN}/getUpdates`

**메시지 포맷 (MarkdownV2):**
```
💻 *\[GitHub\] PR Review Required*

코드 리뷰 요청이 들어왔습니다\.

Source: `GITHUB` Priority: `HIGH`
[View Original](https://github\.com/\.\.\.)
```

**전달 API:** `POST https://api.telegram.org/bot{TOKEN}/sendMessage`

**에러 처리:**
- HTTP 429 → retryable=true / HTTP 400 → retryable=false

---

### 5.3 Discord

**인증 방식:** Webhook URL (`https://discord.com/api/webhooks/{id}/{token}`)

**Webhook 설정 절차:**
1. Discord 채널 → 설정 → 연동 → 웹후크 → 새 웹후크
2. Webhook URL 복사

**메시지 포맷 (Embeds):**
```json
{
  "embeds": [{
    "title": "💻 [GitHub] PR Review Required",
    "description": "코드 리뷰 요청이 들어왔습니다.",
    "color": 16744448,
    "fields": [
      {"name": "Source",   "value": "GITHUB", "inline": true},
      {"name": "Priority", "value": "HIGH",   "inline": true}
    ],
    "url": "https://github.com/...",
    "timestamp": "2026-05-12T10:00:00Z"
  }]
}
```

**전달 API:** `POST {WEBHOOK_URL}?wait=true`
**검증 API:** `GET {WEBHOOK_URL}`

**에러 처리:**
- HTTP 429 → retryable=true / HTTP 400/401/404 → retryable=false / HTTP 5xx → retryable=true

---

## 6. 라우팅 규칙 엔진 + Digest 모드

### 6.1 설계 원칙

- 알림 수신 시 ChannelRouter가 사용자의 RoutingRule 목록을 `priority_order` 순으로 평가
- 조건 매칭 시 해당 규칙의 `delivery_mode`에 따라 즉시 전달 또는 Digest 큐잉
- `stop_on_match=true`인 규칙 매칭 후 이후 규칙 평가 중단
- 매칭 규칙 없으면 채널 전달 없음 (대시보드에만 표시)

### 6.2 RoutingCondition 구조

```json
{
  "sources": ["GITHUB", "CLAUDE"],
  "priorities": ["HIGH", "URGENT"]
}
```

- `sources` null/빈 배열 → 모든 소스
- `priorities` null/빈 배열 → 모든 우선순위
- 두 조건 AND 관계

### 6.3 Delivery Mode

| 모드 | 동작 | digest_interval_min |
|------|------|---------------------|
| `IMMEDIATE` | 즉시 채널 전달 | null |
| `DIGEST` | 알림 누적 후 만료 시 묶음 전송 | 10 / 20 / 30 / 60 등 |

### 6.4 IMMEDIATE 라우팅 시나리오

```
사용자 설정:
  규칙 1 (order=10): source=GITHUB, priority=URGENT → [Slack채널] (stop=true, IMMEDIATE)
  규칙 2 (order=20): any + HIGH → [Slack채널, Telegram채널] (IMMEDIATE)
  규칙 3 (order=99): any + any → [Discord채널] catch-all (DIGEST, 20분)

알림 수신:
  GITHUB + URGENT → 규칙1 IMMEDIATE → Slack 즉시 전달
  CLAUDE + HIGH   → 규칙2 IMMEDIATE → Slack + Telegram 즉시 전달
  CODEX  + LOW    → 규칙3 DIGEST    → Discord 큐잉 (20분 후 묶음)
```

### 6.5 DIGEST 윈도우 동작

```
22:00:05  Codex 알림 A 도착 → DIGEST_PENDING (next_delivery_at = 22:20:05)
22:08:30  Codex 알림 B 도착 → DIGEST_PENDING (next_delivery_at = 22:20:05 유지)
22:13:10  Codex 알림 C 도착 → DIGEST_PENDING (next_delivery_at = 22:20:05 유지)

22:20:05  NotificationDigestScheduler 실행
          → 알림 A, B, C 그룹화
          → buildDigestSummaryPrompt([A, B, C])
          → LlmProvider.chat() → 묶음 요약 메시지
          → Discord에 메시지 1개 전송
          → 3개 ChannelDeliveryLog → SUCCESS
```

**윈도우 시작 기준**: 해당 채널에 첫 번째 DIGEST_PENDING 로그 생성 시각.
이후 같은 채널로 오는 알림은 기존 `next_delivery_at`을 변경하지 않고 새 로그만 추가.

### 6.6 재시도 정책 (IMMEDIATE 전달 실패)

```
retryable = true (429/503): 지수 백오프
  1회 실패 → next_retry_at = now + 1분
  2회 실패 → now + 5분
  3회 실패 → now + 25분
  4회 실패 → status = DEAD

retryable = false (400/401/403): 즉시 DEAD

ChannelDeliveryScheduler: 5분 주기, status=RETRY AND next_retry_at <= now 최대 50개 처리
```

---

## 7. LLM 전략 — 알림 요약 파이프라인

### 7.1 설계 목표

LLM+RAG를 인터랙티브 채팅이 아닌 **알림 요약 파이프라인 전용**으로 사용.
Codex/Claude Code와 같은 AI 에이전트가 생성한 장문·구조적 출력을 채널 전달에 적합한 짧은 메시지로 변환.

### 7.2 Provider 구성

```yaml
# application.yml
notio:
  ai:
    provider: ${NOTIO_AI_PROVIDER:ollama}         # ollama | anthropic | openai
    summarize-sources: ${NOTIO_AI_SUMMARIZE_SOURCES:CLAUDE,CODEX}
    # 빈 문자열 = 모든 소스 요약
    llm-timeout: ${NOTIO_AI_LLM_TIMEOUT:20s}
```

```
LlmProvider (interface)
├── OllamaLlmProvider      @ConditionalOnProperty(havingValue="ollama", matchIfMissing=true)
├── AnthropicLlmProvider   @ConditionalOnProperty(havingValue="anthropic")
└── OpenAiLlmProvider      @ConditionalOnProperty(havingValue="openai")  [선택 구현]
```

### 7.3 PromptBuilder 변경

| 메서드 | v2.0 | v2.1 |
|--------|------|------|
| `buildChatPrompt()` | 채팅 응답 프롬프트 | **삭제** |
| `buildDailySummaryPrompt()` | 일일 요약 프롬프트 | **삭제** |
| `buildNotificationSummaryPrompt(notification, ragContext)` | — | **신규** |
| `buildDigestSummaryPrompt(notifications)` | — | **신규** |

**buildNotificationSummaryPrompt 요건:**
- 시스템: "당신은 개발자 알림을 채팅 플랫폼용으로 간결하게 요약하는 AI입니다."
- 컨텍스트: 유사 과거 알림 (RAG documents) — 중복/반복 알림 식별용
- 입력: 알림 source, title, body, priority, externalUrl
- 출력: 2-4문장, 핵심 내용 + 필요 조치 포함, 마크다운 사용 가능
- 최대 출력: 500자

**buildDigestSummaryPrompt 요건:**
- 시스템: "당신은 여러 개발자 알림을 하나의 묶음 요약으로 작성하는 AI입니다."
- 입력: 복수 알림 목록 (source, title, body, priority)
- 출력: 전체 요약 1-2문장 + 알림별 1줄 요약 목록
- 최대 출력: 1000자

### 7.4 ChatMetrics 의존 해소

`chat/` 패키지 삭제 시 아래 컴포넌트의 `ChatMetrics` 의존이 깨짐:

| 컴포넌트 | 조치 |
|---------|------|
| `PgvectorRagRetriever` | `ChatMetrics` → `NotificationFlowMetrics.recordRagRetrieval()` 대체 |
| `OllamaLlmProvider` | `ChatMetrics` → `ai/metrics/LlmMetrics` (신규, `ai/` 패키지 내) |
| 스트리밍 전용 메트릭 | `recordFirstChunk`, `incrementActiveStreams` 삭제 (스트리밍 채팅 없음) |

### 7.5 LLM 옵션 비교

| 기준 | Ollama (llama3.2:3b) | Claude Haiku | Claude Sonnet |
|------|---------------------|--------------|---------------|
| 응답 속도 | 5-30초 (VPS CPU) | 1-3초 | 2-5초 |
| 요약 품질 | 보통 | 우수 | 최상 |
| 월 비용 (개인) | €0 | $3-5 | $15-30 |
| 프라이버시 | 완전 로컬 | API 전송 | API 전송 |

**권장 전략:**
- 초기: Ollama 유지 (비용 무료, 프라이버시)
- 요약 품질 개선 필요 시: `NOTIO_AI_PROVIDER=anthropic`으로 전환

---

## 8. DB 스키마 변경 (V12–V14)

### V12: push → channel 전환 (v2.0 그대로)

```sql
-- V12__migrate_push_to_channels.sql
ALTER TABLE devices RENAME TO devices_deprecated;

CREATE TABLE notification_channels (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    channel_type         VARCHAR(20) NOT NULL,
    display_name         VARCHAR(100) NOT NULL,
    credential_encrypted TEXT NOT NULL,
    target_identifier    VARCHAR(255),
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_error           TEXT,
    error_count          INT NOT NULL DEFAULT 0,
    last_delivered_at    TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT fk_notification_channels_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_channel_type CHECK (channel_type IN ('SLACK', 'TELEGRAM', 'DISCORD')),
    CONSTRAINT chk_channel_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ERROR'))
);

CREATE INDEX idx_notification_channels_user_id
    ON notification_channels(user_id) WHERE deleted_at IS NULL;

CREATE TABLE routing_rules (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    rule_name       VARCHAR(100) NOT NULL,
    priority_order  INT NOT NULL DEFAULT 100,
    conditions      JSONB NOT NULL DEFAULT '{}',
    channel_ids     JSONB NOT NULL DEFAULT '[]',
    stop_on_match   BOOLEAN NOT NULL DEFAULT TRUE,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_routing_rules_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_routing_rules_user_priority
    ON routing_rules(user_id, priority_order)
    WHERE is_enabled = TRUE AND deleted_at IS NULL;
```

### V13: 채널 전달 로그 (v2.0 그대로)

```sql
-- V13__create_channel_delivery_logs.sql
CREATE TABLE channel_delivery_logs (
    id                   BIGSERIAL PRIMARY KEY,
    notification_id      BIGINT NOT NULL,
    channel_id           BIGINT NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count        INT NOT NULL DEFAULT 0,
    last_error           TEXT,
    external_message_id  VARCHAR(255),
    next_retry_at        TIMESTAMPTZ,
    delivered_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_logs_notifications FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_delivery_logs_channels FOREIGN KEY (channel_id) REFERENCES notification_channels(id),
    CONSTRAINT chk_delivery_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'DEAD'))
);

CREATE INDEX idx_delivery_logs_notification_id ON channel_delivery_logs(notification_id);
CREATE INDEX idx_delivery_logs_retry
    ON channel_delivery_logs(next_retry_at)
    WHERE status = 'RETRY' AND next_retry_at IS NOT NULL;

CREATE UNIQUE INDEX uq_delivery_logs_active
    ON channel_delivery_logs(notification_id, channel_id)
    WHERE status IN ('PENDING', 'RETRY');
```

### V14: AI 요약 + Digest 모드 + chat_messages 폐기 (신규)

```sql
-- V14__add_ai_summary_digest_mode.sql

-- 1. notifications 테이블에 ai_summary 추가
ALTER TABLE notifications ADD COLUMN ai_summary TEXT;
COMMENT ON COLUMN notifications.ai_summary IS
  'LLM-generated concise summary. NULL if summarization is disabled/failed. '
  'ChannelRouter uses COALESCE(ai_summary, body) for channel delivery.';

-- 2. routing_rules에 delivery_mode 추가
ALTER TABLE routing_rules
    ADD COLUMN delivery_mode    VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE',
    ADD COLUMN digest_interval_min INT NULL,
    ADD CONSTRAINT chk_delivery_mode
        CHECK (delivery_mode IN ('IMMEDIATE', 'DIGEST'));

-- 3. channel_delivery_logs에 DIGEST_PENDING 상태 추가
ALTER TABLE channel_delivery_logs DROP CONSTRAINT chk_delivery_status;
ALTER TABLE channel_delivery_logs
    ADD CONSTRAINT chk_delivery_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'DEAD', 'DIGEST_PENDING'));

-- UNIQUE 제약도 DIGEST_PENDING 포함으로 재생성
DROP INDEX uq_delivery_logs_active;
CREATE UNIQUE INDEX uq_delivery_logs_active
    ON channel_delivery_logs(notification_id, channel_id)
    WHERE status IN ('PENDING', 'RETRY', 'DIGEST_PENDING');

-- Digest 스케줄러 인덱스
CREATE INDEX idx_delivery_logs_digest_pending
    ON channel_delivery_logs(channel_id, next_retry_at)
    WHERE status = 'DIGEST_PENDING';

-- Delivery Feed 쿼리 성능 인덱스
CREATE INDEX idx_delivery_logs_delivered_at
    ON channel_delivery_logs(delivered_at DESC)
    WHERE status = 'SUCCESS';

-- 4. chat_messages 폐기
ALTER TABLE chat_messages RENAME TO chat_messages_deprecated;
COMMENT ON TABLE chat_messages_deprecated IS
  'Deprecated: AI interactive chat history. Superseded by channel delivery feed. '
  'Scheduled drop after 2026-08-12 (V15).';
```

### 인덱스 전략 전체

| 테이블 | 인덱스 | 사용 쿼리 |
|--------|--------|-----------|
| `notification_channels` | `(user_id) WHERE deleted_at IS NULL` | 사용자 활성 채널 목록 |
| `routing_rules` | `(user_id, priority_order) WHERE enabled` | 알림 수신 시 규칙 평가 |
| `channel_delivery_logs` | `(next_retry_at) WHERE status='RETRY'` | 재시도 스케줄러 |
| `channel_delivery_logs` | `(channel_id, next_retry_at) WHERE status='DIGEST_PENDING'` | Digest 스케줄러 |
| `channel_delivery_logs` | `(delivered_at DESC) WHERE status='SUCCESS'` | Delivery Feed API |
| `channel_delivery_logs` | UNIQUE `(notification_id, channel_id) WHERE IN ('PENDING','RETRY','DIGEST_PENDING')` | 중복 방지 |

---

## 9. Frontend — Flutter Web 대시보드

### 9.1 전환 전략

기존 189개 Dart 파일 최대한 활용. Clean Architecture 구조 유지.
`chat/` feature를 `delivery_feed/` feature로 완전 교체.

### 9.2 제거할 코드

| 대상 | 이유 |
|------|------|
| `firebase_core`, `firebase_messaging` 패키지 | FCM 불필요 |
| `flutter_local_notifications` 패키지 | 웹 미지원 |
| `sqlite3_flutter_libs` 패키지 | Flutter Web 빌드 블로커 |
| `features/chat/` 전체 | AI 채팅 불필요 |
| `core/database/tables/chat_message_table.dart` | chat_messages 테이블 폐기 |

### 9.3 신규 `delivery_feed/` feature

```
lib/features/delivery_feed/
├── data/
│   ├── datasource/
│   │   └── delivery_feed_remote_datasource.dart    # GET /api/v1/channels/delivery-feed
│   └── model/
│       └── delivery_feed_item_model.dart
├── domain/
│   ├── entity/
│   │   └── delivery_feed_item_entity.dart
│   └── repository/
│       └── delivery_feed_repository.dart
└── presentation/
    ├── providers/
    │   ├── delivery_feed_notifier.dart
    │   ├── delivery_feed_providers.dart
    │   └── delivery_feed_state.dart
    └── screens/
    │   └── delivery_feed_screen.dart               # /chat 라우트 담당
    └── widgets/
        ├── delivery_bubble.dart                    # 채널 아이콘 + 요약 + 타임스탬프
        └── channel_filter_chips.dart               # All / Slack / Telegram / Discord
```

**DeliveryFeedState:**
```dart
class DeliveryFeedState {
  final List<DeliveryFeedItemEntity> items;
  final bool isLoading;
  final bool isLoadingMore;
  final bool hasMore;
  final int page;
  final ChannelType? filter;    // null = All
  final String? error;
}
```

**DeliveryFeedScreen 주요 기능:**
- AppBar: "Deliveries"
- 각 `DeliveryBubble`: 채널 아이콘, 채널 표시명, 원본 알림 제목, 요약 본문, 전달 시각
- 탭 → `/notifications/:id` 상세 화면 이동
- 필터 칩: All / Slack / Telegram / Discord
- 무한 스크롤 (80% 지점에서 다음 페이지 로드)
- Pull to refresh
- **빈 상태**: "전달된 알림이 없습니다. 설정 → 채널 관리에서 채널을 추가하세요."

### 9.4 반응형 레이아웃

```
모바일 (< 800px):              웹 대시보드 (≥ 800px):
┌────────────────────┐         ┌──────┬──────────────────────┐
│                    │         │  🔔  │                      │
│    Main Content    │         │  📨  │   Main Content       │
│                    │         │  📊  │                      │
├──┬──┬──┬───────────┤         │  ⚙️  │  (Detail Panel)      │
│🔔│📨│📊│⚙️         │         └──────┴──────────────────────┘
└──┴──┴──┴───────────┘         NavigationRail
BottomNavigationBar
```

분기 로직: `MediaQuery.of(context).size.width >= 800`

### 9.5 라우트 구조

```dart
class Routes {
  // 기존 유지
  static const String notifications = '/notifications';
  static const String chat          = '/chat';            // delivery feed로 재사용
  static const String analytics     = '/analytics';
  static const String settings      = '/settings';
  static const String connections   = '/settings/connections';

  // 신규 추가
  static const String channels      = '/channels';        // 채널 관리
  static const String routingRules  = '/routing-rules';   // 라우팅 규칙
}
```

**바텀 네비게이션 변경:**

| 탭 인덱스 | v2.0 | v2.1 |
|----------|------|------|
| 0 | Notifications (🔔) | Notifications (🔔) — 변경 없음 |
| 1 | Chat (💬) | **Deliveries (📨)** |
| 2 | Analytics (📊) | Analytics (📊) — 변경 없음 |
| 3 | Settings (⚙️) | Settings (⚙️) — 변경 없음 |

**채널 설정 접근**: Settings 화면에서 서브화면으로 이동 (탭 추가 없음)
- "채널 관리" → `/channels`
- "라우팅 규칙" → `/routing-rules`
- 기존 "연동 관리" → `/settings/connections` 패턴과 동일

### 9.6 신규 `channels/` feature

```
lib/features/channels/
├── data/
│   ├── datasource/channel_remote_datasource.dart
│   └── model/
│       ├── notification_channel_model.dart
│       └── routing_rule_model.dart
├── domain/
│   ├── entity/
│   │   ├── notification_channel_entity.dart
│   │   └── routing_rule_entity.dart
│   └── repository/channel_repository.dart
└── presentation/
    ├── providers/channel_providers.dart
    └── screens/
        ├── channels_screen.dart           # 채널 목록, 상태, 마지막 전달
        ├── channel_create_screen.dart     # 채널 타입 선택 → 자격증명 입력 → 검증
        └── routing_rules_screen.dart      # 규칙 CRUD, drag-to-reorder
```

**channels_screen 주요 기능:**
- 채널 카드: 이름, 타입 아이콘, 상태 배지, 마지막 전달 시각
- 오류 채널 하이라이트 (error_count 표시)
- 채널 활성화/일시중지 토글
- 테스트 전송 버튼

**routing_rules_screen 주요 기능:**
- 규칙 목록 (priority_order 순)
- 소스/우선순위 멀티셀렉트 필터 칩
- 대상 채널 멀티셀렉트
- stop_on_match 토글
- **전달 방식 선택**: 즉시 전송 / 묶음 전송 (`SegmentedButton`)
- **묶음 간격 선택** (묶음 전송 시): 10분 / 20분 / 30분 / 1시간

### 9.7 SSE 실시간 업데이트 (FCM 대체)

```
Backend:
  GET /api/v1/notifications/stream  (text/event-stream)
  → SseEmitter per userId
  → 알림 생성 시 해당 userId emitter에 push

Frontend:
  RealtimeNotificationService (웹 전용, kIsWeb guard)
  → 새 알림 이벤트 수신 시 Riverpod provider invalidate
  → 알림 목록 자동 갱신
```

### 9.8 Flutter Web 빌드

```bash
flutter build web \
  --release \
  --web-renderer html \
  --dart-define=API_BASE_URL=https://your-domain.com

# 출력: frontend/build/web/
```

---

## 10. 인프라 및 배포

### 10.1 VPS 선택 기준

| | Ollama 포함 | Ollama 미포함 (Claude API) |
|---|---|---|
| 권장 VPS | Hetzner CX32 (4vCPU / 8GB) | Hetzner CX22 (2vCPU / 4GB) |
| 월 비용 | ~€10 | ~€4 + API $3-5 |

### 10.2 Docker Compose 구성

```yaml
services:
  nginx:            # 80/443, Flutter Web + API 프록시
  certbot:          # Let's Encrypt 자동 갱신 (12시간 주기)
  notio-backend:    # expose 8080 (외부 직접 노출 없음)
  postgres:         # expose 5432 (내부망만)
  redis:            # expose 6379 (내부망만)
  ollama:           # expose 11434 (내부망만), profiles: [with-ollama]

volumes:
  postgres_data, redis_data, ollama_data
  notio_web_dist
  certbot_webroot

networks:
  notio-network: bridge
```

### 10.3 Nginx 핵심 설정

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    root /var/www/notio-web;
    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://notio-backend:8080;
        proxy_buffering off;
        proxy_read_timeout 3600s;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator/ {
        allow 127.0.0.1;
        deny all;
        proxy_pass http://notio-backend:8080;
    }
}
```

### 10.4 환경변수 목록

| 변수 | 설명 | 비고 |
|------|------|------|
| `NOTIO_AI_PROVIDER` | `ollama` \| `anthropic` \| `openai` | 신규 |
| `NOTIO_AI_SUMMARIZE_SOURCES` | `CLAUDE,CODEX` (콤마 구분) | 신규 (빈 값=전체) |
| `ANTHROPIC_API_KEY` | Anthropic API 키 | 신규 |
| `NOTIO_AI_LLM_TIMEOUT` | LLM 타임아웃 (기본: 20s) | 신규 |
| `NOTIO_CREDENTIAL_ENCRYPTION_KEY` | 채널 Bot Token AES-256 암호화 키 | 기존 활용 |

---

## 11. Connection vs Channel 개념 구분

| 개념 | Connection (Inbound) | Channel (Outbound) |
|------|---------------------|-------------------|
| **방향** | 외부 → Notio | Notio → 외부 |
| **역할** | 알림 수신 소스 | 알림 전달 대상 |
| **인증** | HMAC Webhook Secret / OAuth | Bot Token / Webhook URL |
| **DB** | `connections`, `connection_credentials` | `notification_channels` |
| **패키지** | `com/notio/connection/` | `com/notio/channel/` |
| **예시** | GitHub가 Notio에 Push 이벤트 전송 | Notio가 Slack에 알림 메시지 전송 |

**사용자 설정 관점:**
- "연동 관리 (Connections)": 알림을 어디서 받을지 (GitHub, Slack, Gmail...)
- "채널 관리 (Channels)": 알림을 어디로 보낼지 (Slack, Telegram, Discord)

---

## 12. 보안 설계

### 12.1 Bot Token / Webhook URL 보호

- 저장: `CredentialEncryptionService` (AES-256-GCM) 재사용
- 응답: `ChannelResponse`에 credential 미포함, `keyPreview` (마지막 4자리)만 노출
- 전달 시: decrypt → Provider 호출 → 메모리 즉시 폐기

### 12.2 CORS / HTTPS / Actuator 보호

- CORS: `notio.cors.allowed-origins: ["https://your-domain.com"]`
- HTTP(80) → HTTPS(443) 강제 리다이렉트
- `/actuator/` → 내부망(127.0.0.1) 전용 차단

### 12.3 Bot Token 유효성 검증 흐름

```
채널 생성 요청
  → Provider.validate(credentialPlaintext, targetIdentifier)
      Slack: POST /api/auth.test
      Telegram: GET /bot{TOKEN}/getMe
      Discord: GET {WEBHOOK_URL}
  ├─ invalid → 400 CHANNEL_CREDENTIAL_INVALID
  └─ valid → encrypt → DB 저장
```

---

## 13. 운영 및 모니터링

### 13.1 AI 요약 메트릭

```
notio_ai_summarization_total{outcome="success"}
notio_ai_summarization_total{outcome="failure"}
notio_ai_summarization_duration_seconds
notio_rag_retrieval_total{time_range_applied="false"}
notio_rag_retrieval_duration_seconds
```

### 13.2 채널 전달 메트릭

```
notio_channel_delivery_total{channel_type="slack",    outcome="success"}
notio_channel_delivery_total{channel_type="telegram", outcome="failure"}
notio_channel_delivery_total{channel_type="discord",  outcome="retry"}
notio_channel_delivery_duration_seconds{channel_type="slack"}
notio_channel_delivery_retry_total{channel_type="telegram", attempt="2"}
notio_digest_delivery_total{channel_type="discord", notification_count="3"}
```

### 13.3 장애 격리 보장

```
채널/LLM 장애 시에도:
  ✅ 알림 DB 저장 정상
  ✅ 대시보드 알림 목록 표시 정상
  ✅ Delivery Feed에 실패 항목 표시
  ✅ 알림 생성 HTTP 응답 정상 (비동기 처리)

  ❌ 해당 채널로만 전달 실패 (ChannelDeliveryLog 기록)
  ❌ LLM 실패 시 원본 body로 채널 전달 (graceful degradation)
```

### 13.4 백업 전략

```bash
# 일별 백업 (cron: 0 3 * * *)
docker exec notio-postgres pg_dump \
  -U $DB_USERNAME -d notio \
  --format=custom \
  -f /tmp/notio_$(date +%Y%m%d).dump
```

---

## 14. Phase 로드맵

### Phase 1: Backend 채널 + 요약 파이프라인 (3주)

**Week 1 — 핵심 백엔드**
- `chat/` 패키지 전체 삭제
- `PromptBuilder` 수정: `buildNotificationSummaryPrompt()`, `buildDigestSummaryPrompt()` 추가
- `Notification` 엔티티 `aiSummary` 필드 추가, `updateAiSummary()` Repository 메서드
- `NotificationSummaryService` 구현
- `PgvectorRagRetriever` — `ChatMetrics` → `LlmMetrics` / `NotificationFlowMetrics` 대체
- `NotificationService` 수정 — 요약 + 채널 라우팅 비동기 오케스트레이션
- V14 마이그레이션 SQL 작성

**Week 2 — 채널 모듈 + Digest**
- `channel/domain/` 엔티티 및 enum 구현
- `SlackChannelProvider`, `TelegramChannelProvider`, `DiscordChannelProvider` 구현
- `ChannelRouter` (IMMEDIATE), `DigestChannelRouter` (DIGEST) 구현
- `ChannelDeliveryScheduler` (RETRY 백오프), `NotificationDigestScheduler` (Digest 윈도우) 구현
- `NotificationChannelController`, `RoutingRuleController` REST API
- `DeliveryFeedController` + `DeliveryFeedItem` DTO
- `AnthropicLlmProvider` + `@ConditionalOnProperty` 분기
- 단위/통합 테스트

**Week 3 — Frontend**
- `chat/` feature 전체 Dart 파일 삭제 (Drift chat_message_table 포함)
- `delivery_feed/` feature 구현 (datasource, repository, providers, screen, widgets)
- `app_router.dart`: `/channels`, `/routing-rules` 라우트 추가, ChatScreen → DeliveryFeedScreen 교체
- 바텀 네비게이션 탭2 "Deliveries" 변경
- `settings_screen.dart`: 채널 관리 / 라우팅 규칙 메뉴 추가
- `features/channels/` 전체 구현
- `routing_rules_screen.dart`: Digest 옵션 추가 (즉시/10분/20분/30분/1시간 `SegmentedButton`)

### Phase 2: 인프라 배포 (1주)

**Week 4**
- Hetzner VPS 프로비저닝
- Let's Encrypt SSL 인증서 발급
- Docker Compose 프로덕션 배포 (`docker-compose.prod.yml`)
- Flutter Web 빌드 + Nginx 서빙 연동
- Prometheus + Grafana 모니터링 검증
- 전체 E2E 테스트 (Webhook 수신 → LLM 요약 → Slack/Telegram/Discord 전달)

### Phase 3 이후: 고도화 (필요 시)

| 목표 | 내용 |
|------|------|
| 이메일 채널 추가 | `EmailChannelProvider` (인터페이스 구현만으로 추가) |
| Claude API 전환 | 환경변수 변경만으로 무중단 전환 |
| Digest 채널별 설정 | 현재 RoutingRule 단위 → 채널 단위 세분화 |
| MSA 진화 | Kafka, 서비스 분리 (v1.0 Phase 2-4 로드맵) |

---

## 15. 기술 의사결정 기록 (ADR)

### ADR-001: FCM → 채팅 플랫폼 알림 전달

**결정:** FCM/APNs 모바일 푸시 제거 → Slack/Telegram/Discord 전달

**근거:** App Store 배포 불필요, 개발자 타겟 사용자는 이미 채팅 플랫폼 상시 접속

**트레이드오프:** 채팅 플랫폼 계정 필요

---

### ADR-002: Flutter Web (기존 코드 재활용)

**결정:** 기존 189개 Dart 파일을 Flutter Web으로 전환

**근거:** Clean Architecture가 웹에서도 동일 동작, 새 프레임워크 도입 시 4-6주 추가 개발

**트레이드오프:** 초기 번들 크기 ~3-5MB

---

### ADR-003: Ollama 유지 + Cloud API 옵션

**결정:** 기본 로컬 Ollama, 환경변수로 Claude API 전환

**근거:** 기존 `LlmProvider` 인터페이스로 코드 변경 없이 교환 가능

**트레이드오프:** Ollama VPS 메모리 4GB 추가 필요

---

### ADR-004: 채널 전달 비동기 처리

**결정:** `ChannelRouter.route()`를 비동기 호출

**근거:** 채널 장애가 알림 생성 HTTP 응답을 블로킹하면 안 됨

**트레이드오프:** 알림 생성과 전달 사이 수십 ms 지연

---

### ADR-005: AI 인터랙티브 채팅 제거 → 요약 파이프라인 전환

**결정:** `chat/` 패키지 전체 제거, LLM+RAG를 알림 요약 전용으로 전환

**근거:**
- Codex/Claude Code 알림은 이미 AI가 생성한 텍스트 — 재질문 구조 불합리
- 인터랙티브 채팅 (SSE 스트리밍, 대화이력, RAG+히스토리)의 복잡도가 핵심 가치 대비 과다
- LLM을 요약 파이프라인 전용으로 단순화하면 더 예측 가능하고 운영 가능한 시스템

**트레이드오프:** 사용자가 알림 내용을 직접 질의하는 방법 없음
→ 대시보드의 알림 목록 + 상세 화면으로 충분히 대체 가능

---

### ADR-006: Delivery Feed — 채팅 화면 재활용

**결정:** `/chat` 라우트와 채팅 버블 UI 패턴을 채널 전달 피드로 재활용

**근거:**
- 채팅 버블 UI가 "전달된 메시지 확인" 패턴과 시각적으로 자연스러운 매핑
- 기존 라우트(`/chat`) 유지로 딥링크 변경 없음
- ChannelDeliveryLog + Notification 조인으로 필요 데이터 제공 가능

**트레이드오프:** 사용자가 처음에 "채팅"으로 인식할 수 있는 탭이 "Deliveries"로 변경됨
→ 탭 아이콘 변경 (💬 → 📨)과 AppBar 제목으로 충분히 구분 가능

---

### ADR-007: Digest 모드 — RoutingRule 단위 설정

**결정:** 전달 타이밍(즉시/묶음)을 RoutingRule 단위로 설정

**근거:**
- 사용자가 소스/우선순위별로 다른 전달 타이밍 적용 가능 (URGENT → 즉시, 나머지 → 20분 묶음)
- 기존 RoutingRule 구조에 2개 컬럼(`delivery_mode`, `digest_interval_min`) 추가로 최소 변경
- Digest 윈도우는 첫 번째 알림 도착 시 시작 → 직관적 동작

**트레이드오프:**
- LLM 호출이 Digest 스케줄러 실행 시 발생 → 스케줄러 실행 시간 증가 가능
  → Digest 묶음당 LLM 1회 호출 (개별 요약 대비 효율적)
- 동시에 여러 채널의 Digest 만료 시 병렬 LLM 호출 → 적절한 스레드 풀 설정 필요

---

**문서 끝**

이 Blueprint v2.1은 Notio를 인터랙티브 AI 채팅 허브에서
**LLM 요약 파이프라인 기반 스마트 알림 전달 허브**로 전환하는 설계를 정의합니다.
