# Plan: Backend 개발

> **대상 버전**: v2.1
> **작성일**: 2026-05-12
> **연관 Blueprint**: `docs/blueprint/notio_blueprint_v2.md` §2, §4, §6, §8

---

## 개요

v2.1 백엔드 변경의 핵심은 세 가지다:
1. `chat/` + `push/` 패키지 제거
2. `channel/` 모듈 신규 구현 (Slack/Telegram/Discord 전달 + Digest 모드)
3. `notification/` 패키지 — 요약 파이프라인 연동 + Delivery Feed API 추가

AI 관련 변경 (`NotificationSummaryService`, `PromptBuilder` 등)은 `plan_ai.md` 참조.

---

## Phase 1: 패키지 제거

### 1-1. `push/` 패키지 전체 삭제

```
backend/src/main/java/com/notio/push/
├── controller/DeviceController.java
├── domain/Device.java
├── domain/DevicePlatform.java
├── dto/DeviceResponse.java
├── dto/RegisterDeviceRequest.java
├── repository/DeviceRepository.java
└── service/
    ├── DeviceService.java
    └── PushService.java
```

**삭제 전 확인:**
- `NotificationService.java`에서 `PushService` 의존 제거 (Phase 3에서 처리)
- `FirebaseConfig.java` 삭제 (`common/config/`)
- `build.gradle`에서 `firebase-admin` 의존성 제거

```kotlin
// build.gradle.kts — 제거
// implementation("com.google.firebase:firebase-admin:9.4.2")
```

### 1-2. `chat/` 패키지 삭제

`plan_ai.md` Phase 1 완료 후 삭제.
삭제 전 `ChatMetrics` 의존 해소 필수 (plan_ai.md §1-2 참조).

---

## Phase 2: DB 마이그레이션 작성

### 2-1. V12 — push → channel 전환

**파일**: `resources/db/migration/V12__migrate_push_to_channels.sql`

```sql
-- devices 테이블 보관 (즉시 DROP하지 않음, 90일 후 삭제 예정)
ALTER TABLE devices RENAME TO devices_deprecated;
COMMENT ON TABLE devices_deprecated IS
  'Deprecated: FCM device table. Migrated 2026-05-12. Drop after 2026-08-12.';

-- 채널 설정 테이블
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
    CONSTRAINT fk_notification_channels_users
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_channel_type
        CHECK (channel_type IN ('SLACK', 'TELEGRAM', 'DISCORD')),
    CONSTRAINT chk_channel_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ERROR'))
);

CREATE INDEX idx_notification_channels_user_id
    ON notification_channels(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_notification_channels_status
    ON notification_channels(status) WHERE deleted_at IS NULL;

-- 라우팅 규칙 테이블
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
    CONSTRAINT fk_routing_rules_users
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_routing_rules_user_priority
    ON routing_rules(user_id, priority_order)
    WHERE is_enabled = TRUE AND deleted_at IS NULL;
```

### 2-2. V13 — 채널 전달 로그

**파일**: `resources/db/migration/V13__create_channel_delivery_logs.sql`

```sql
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
    CONSTRAINT fk_delivery_logs_notifications
        FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_delivery_logs_channels
        FOREIGN KEY (channel_id) REFERENCES notification_channels(id),
    CONSTRAINT chk_delivery_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'DEAD'))
);

CREATE INDEX idx_delivery_logs_notification_id
    ON channel_delivery_logs(notification_id);
CREATE INDEX idx_delivery_logs_retry
    ON channel_delivery_logs(next_retry_at)
    WHERE status = 'RETRY' AND next_retry_at IS NOT NULL;

CREATE UNIQUE INDEX uq_delivery_logs_active
    ON channel_delivery_logs(notification_id, channel_id)
    WHERE status IN ('PENDING', 'RETRY');
```

### 2-3. V14 — AI 요약 + Digest 모드 + chat_messages 폐기

**파일**: `resources/db/migration/V14__add_ai_summary_digest_mode.sql`

```sql
-- 1. notifications.ai_summary 추가
ALTER TABLE notifications ADD COLUMN ai_summary TEXT;
COMMENT ON COLUMN notifications.ai_summary IS
  'LLM-generated concise summary for channel delivery. '
  'NULL when summarization is disabled/failed. '
  'ChannelRouter uses COALESCE(ai_summary, body).';

-- 2. routing_rules — Digest 모드 컬럼 추가
ALTER TABLE routing_rules
    ADD COLUMN delivery_mode      VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE',
    ADD COLUMN digest_interval_min INT NULL;

ALTER TABLE routing_rules
    ADD CONSTRAINT chk_delivery_mode
        CHECK (delivery_mode IN ('IMMEDIATE', 'DIGEST'));

-- 3. channel_delivery_logs — DIGEST_PENDING 상태 추가
ALTER TABLE channel_delivery_logs DROP CONSTRAINT chk_delivery_status;
ALTER TABLE channel_delivery_logs
    ADD CONSTRAINT chk_delivery_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'DEAD', 'DIGEST_PENDING'));

-- UNIQUE 제약 재생성 (DIGEST_PENDING 포함)
DROP INDEX uq_delivery_logs_active;
CREATE UNIQUE INDEX uq_delivery_logs_active
    ON channel_delivery_logs(notification_id, channel_id)
    WHERE status IN ('PENDING', 'RETRY', 'DIGEST_PENDING');

-- Digest 스케줄러 인덱스
CREATE INDEX idx_delivery_logs_digest_pending
    ON channel_delivery_logs(channel_id, next_retry_at)
    WHERE status = 'DIGEST_PENDING';

-- Delivery Feed API 인덱스
CREATE INDEX idx_delivery_logs_delivered_at
    ON channel_delivery_logs(delivered_at DESC)
    WHERE status = 'SUCCESS';

-- 4. chat_messages 폐기
ALTER TABLE chat_messages RENAME TO chat_messages_deprecated;
COMMENT ON TABLE chat_messages_deprecated IS
  'Deprecated: AI interactive chat history. Scheduled drop V15 after 2026-08-12.';
```

> **중요**: V14는 반드시 `chat/` 패키지 코드 삭제 후 배포 시점에 실행.
> `ChatMessage` 엔티티가 남아있으면 `chat_messages_deprecated` rename 후 Hibernate 기동 실패.

---

## Phase 3: notification/ 패키지 수정

### 3-1. NotificationService 변경

```java
// 변경 전 의존성
private final PushService pushService;

// 변경 후 의존성
private final NotificationSummaryService notificationSummaryService;
private final ChannelRouter channelRouter;
```

비동기 오케스트레이션 로직 (saveNotification 메서드 내):
```java
Notification saved = notificationRepository.save(notification);
evictUnreadCountCache(saved.getUserId());

// Branch A: pgvector 임베딩 (병렬)
CompletableFuture.runAsync(() -> {
    try { notificationEmbeddingService.embedNotification(saved); }
    catch (Exception e) {
        log.warn("event=embedding_failed notification_id={}", saved.getId(), e);
    }
}, virtualThreadExecutor);

// Branch B: LLM 요약 → 채널 라우팅 (순차, Branch A와 병렬)
CompletableFuture.runAsync(() -> {
    try {
        notificationSummaryService.summarize(saved);  // ai_summary DB 저장
        channelRouter.route(saved);                    // COALESCE(ai_summary, body) 사용
    } catch (Exception e) {
        log.error("event=notification_pipeline_failed notification_id={}", saved.getId(), e);
    }
}, virtualThreadExecutor);

return saved;
```

`evictDailySummaryCache()` 메서드 및 호출 완전 제거 (일일 요약 기능 삭제).

### 3-2. NotificationDetailResponse 변경

```java
// 필드 추가
private String aiSummary;   // nullable, LLM 요약본
```

---

## Phase 4: channel/ 모듈 구현

### 4-1. 도메인 엔티티

#### `NotificationChannel.java`

```java
@Entity
@Table(name = "notification_channels")
@SQLDelete(sql = "UPDATE notification_channels SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class NotificationChannel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ChannelType channelType;
    private String displayName;

    @Column(name = "credential_encrypted")
    private String credentialEncrypted;
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    private ChannelStatus status;
    private int errorCount;
    private String lastError;
    private Instant lastDeliveredAt;

    // 비즈니스 메서드
    public void recordSuccess() {
        this.status = ChannelStatus.ACTIVE;
        this.errorCount = 0;
        this.lastDeliveredAt = Instant.now();
    }

    public void recordFailure(String errorMsg) {
        this.errorCount++;
        this.lastError = errorMsg;
        if (this.errorCount >= 5) {
            this.status = ChannelStatus.ERROR;
        }
    }

    public boolean isDeliverable() {
        return this.status == ChannelStatus.ACTIVE;
    }

    public void pause() { this.status = ChannelStatus.PAUSED; }
    public void resume() { this.status = ChannelStatus.ACTIVE; }
}
```

#### `RoutingRule.java`

```java
@Entity
@Table(name = "routing_rules")
public class RoutingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String ruleName;
    private int priorityOrder;

    @Column(columnDefinition = "JSONB")
    @Convert(converter = RoutingConditionConverter.class)
    private RoutingCondition conditions;

    @Column(columnDefinition = "JSONB")
    @Convert(converter = LongListConverter.class)
    private List<Long> channelIds;

    private boolean stopOnMatch;
    private boolean isEnabled;

    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode;      // IMMEDIATE | DIGEST
    private Integer digestIntervalMin;      // null when IMMEDIATE
}
```

#### `ChannelDeliveryLog.java`

```java
@Entity
@Table(name = "channel_delivery_logs")
public class ChannelDeliveryLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long notificationId;
    private Long channelId;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    private int attemptCount;
    private String lastError;
    private String externalMessageId;
    private Instant nextRetryAt;    // RETRY: 백오프 시각 / DIGEST_PENDING: 윈도우 만료
    private Instant deliveredAt;
}
```

#### Enum 클래스

```java
public enum ChannelType    { SLACK, TELEGRAM, DISCORD }
public enum ChannelStatus  { ACTIVE, PAUSED, ERROR }
public enum DeliveryMode   { IMMEDIATE, DIGEST }
public enum DeliveryStatus { PENDING, SUCCESS, FAILED, RETRY, DEAD, DIGEST_PENDING }
```

### 4-2. Provider 인터페이스 및 Value Objects

```java
public interface NotificationChannelProvider {
    ChannelType supports();
    ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message);
    ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier);
}

public record ChannelMessage(
    Long notificationId,
    String title,
    String body,
    NotificationSource source,
    NotificationPriority priority,
    String externalUrl,
    Instant notifiedAt
) {}

public record ChannelDeliveryResult(
    boolean success,
    String externalMessageId,
    String errorMessage,
    boolean retryable
) {
    public static ChannelDeliveryResult success(String messageId) {
        return new ChannelDeliveryResult(true, messageId, null, false);
    }
    public static ChannelDeliveryResult failure(String error, boolean retryable) {
        return new ChannelDeliveryResult(false, null, error, retryable);
    }
}

public record ChannelValidationResult(boolean valid, String errorMessage) {
    public static ChannelValidationResult valid() {
        return new ChannelValidationResult(true, null);
    }
    public static ChannelValidationResult invalid(String msg) {
        return new ChannelValidationResult(false, msg);
    }
}
```

### 4-3. SlackChannelProvider

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackChannelProvider implements NotificationChannelProvider {

    private static final String CHAT_POST_URL = "https://slack.com/api/chat.postMessage";
    private static final String AUTH_TEST_URL  = "https://slack.com/api/auth.test";

    private final RestClient restClient;
    private final CredentialEncryptionService encryptionService;
    private final SlackBlockKitFormatter formatter;

    @Override
    public ChannelType supports() { return ChannelType.SLACK; }

    @Override
    public ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message) {
        String token = encryptionService.decrypt(channel.getCredentialEncrypted());
        Map<String, Object> payload = formatter.format(channel.getTargetIdentifier(), message);
        try {
            Map<String, Object> response = restClient.post()
                .uri(CHAT_POST_URL)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .retrieve()
                .body(Map.class);

            Boolean ok = (Boolean) response.get("ok");
            if (Boolean.TRUE.equals(ok)) {
                return ChannelDeliveryResult.success((String) response.get("ts"));
            }
            String error = (String) response.get("error");
            boolean retryable = "ratelimited".equals(error);
            return ChannelDeliveryResult.failure(error, retryable);

        } catch (HttpClientErrorException e) {
            return ChannelDeliveryResult.failure(e.getMessage(), false);
        } catch (HttpServerErrorException e) {
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        }
    }

    @Override
    public ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier) {
        try {
            Map<String, Object> response = restClient.post()
                .uri(AUTH_TEST_URL)
                .header("Authorization", "Bearer " + credentialPlaintext)
                .retrieve()
                .body(Map.class);
            if (Boolean.TRUE.equals(response.get("ok"))) {
                return ChannelValidationResult.valid();
            }
            return ChannelValidationResult.invalid("Slack auth.test failed: " + response.get("error"));
        } catch (Exception e) {
            return ChannelValidationResult.invalid("Slack validation error: " + e.getMessage());
        }
    }
}
```

**`SlackBlockKitFormatter`**:
- 우선순위별 색상: URGENT `#FF0000`, HIGH `#FF8C00`, MEDIUM `#4A90E2`, LOW `#9B9B9B`
- Block Kit: section(제목), section(본문), context(Source/Priority/링크)

### 4-4. TelegramChannelProvider

핵심 로직:
- `credential_encrypted` = Bot Token
- `target_identifier` = Chat ID
- 전달 API: `POST https://api.telegram.org/bot{TOKEN}/sendMessage`
- `parse_mode: MarkdownV2` — 특수문자 이스케이프 필수: `_ * [ ] ( ) ~ > # + - = | { } . !`
- HTTP 429 → `retryable=true`, HTTP 400 → `retryable=false`
- 검증: `GET https://api.telegram.org/bot{TOKEN}/getMe`

**`TelegramMarkdownFormatter`** — 이스케이프 유틸리티:
```java
public String escape(String text) {
    return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
}
```

### 4-5. DiscordChannelProvider

핵심 로직:
- `credential_encrypted` = Webhook URL 전체 (`https://discord.com/api/webhooks/{id}/{token}`)
- `target_identifier` = null (URL에 포함)
- 전달 API: `POST {WEBHOOK_URL}?wait=true` → message_id 응답
- 검증: `GET {WEBHOOK_URL}`
- HTTP 429 → retryable=true, 400/401/404 → retryable=false, 5xx → retryable=true

**우선순위별 색상 (십진수)**:
- URGENT: 16711680 / HIGH: 16744448 / MEDIUM: 4886754 / LOW: 10197915

### 4-6. ChannelProviderRegistry

```java
@Component
public class ChannelProviderRegistry {
    private final Map<ChannelType, NotificationChannelProvider> registry;

    public ChannelProviderRegistry(List<NotificationChannelProvider> providers) {
        this.registry = providers.stream()
            .collect(Collectors.toMap(NotificationChannelProvider::supports, p -> p));
    }

    public NotificationChannelProvider get(ChannelType type) {
        return Optional.ofNullable(registry.get(type))
            .orElseThrow(() -> new IllegalArgumentException("No provider for: " + type));
    }
}
```

### 4-7. RoutingRuleEvaluator

```java
@Component
public class RoutingRuleEvaluator {

    public boolean matches(RoutingRule rule, Notification notification) {
        RoutingCondition cond = rule.getConditions();
        if (cond == null) return true;

        boolean sourceMatch = isNullOrEmpty(cond.sources()) ||
            cond.sources().contains(notification.getSource().name());
        boolean priorityMatch = isNullOrEmpty(cond.priorities()) ||
            cond.priorities().contains(notification.getPriority().name());

        return sourceMatch && priorityMatch;
    }

    private boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
```

### 4-8. ChannelRouter (IMMEDIATE 경로)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRouter {

    private final RoutingRuleRepository routingRuleRepository;
    private final NotificationChannelRepository channelRepository;
    private final RoutingRuleEvaluator evaluator;
    private final ChannelProviderRegistry providerRegistry;
    private final DigestChannelRouter digestChannelRouter;
    private final ChannelDeliveryLogRepository deliveryLogRepository;
    private final ChannelDeliveryMetrics metrics;

    public void route(Notification notification) {
        List<RoutingRule> rules = routingRuleRepository
            .findByUserIdOrderByPriorityOrder(notification.getUserId());

        for (RoutingRule rule : rules) {
            if (!rule.isEnabled() || !evaluator.matches(rule, notification)) continue;

            List<NotificationChannel> channels = channelRepository
                .findAllById(rule.getChannelIds())
                .stream()
                .filter(NotificationChannel::isDeliverable)
                .toList();

            if (rule.getDeliveryMode() == DeliveryMode.DIGEST) {
                channels.forEach(ch -> digestChannelRouter.queue(notification, ch, rule));
            } else {
                channels.forEach(ch -> deliverImmediate(notification, ch));
            }

            if (rule.isStopOnMatch()) break;
        }
    }

    private void deliverImmediate(Notification notification, NotificationChannel channel) {
        ChannelMessage message = buildMessage(notification);
        NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
        Instant start = Instant.now();
        try {
            ChannelDeliveryResult result = provider.deliver(channel, message);
            Duration duration = Duration.between(start, Instant.now());

            if (result.success()) {
                channel.recordSuccess();
                saveLog(notification.getId(), channel.getId(),
                    DeliveryStatus.SUCCESS, result.externalMessageId(), null, null);
                metrics.recordDelivery(channel.getChannelType().name(), "success", duration);
            } else {
                channel.recordFailure(result.errorMessage());
                DeliveryStatus nextStatus = result.retryable()
                    ? DeliveryStatus.RETRY : DeliveryStatus.DEAD;
                Instant nextRetry = result.retryable() ? computeNextRetry(0) : null;
                saveLog(notification.getId(), channel.getId(),
                    nextStatus, null, result.errorMessage(), nextRetry);
                metrics.recordDelivery(channel.getChannelType().name(), "failure", duration);
            }
        } catch (Exception e) {
            log.error("event=channel_delivery_exception channel_id={}", channel.getId(), e);
            saveLog(notification.getId(), channel.getId(),
                DeliveryStatus.RETRY, null, e.getMessage(), computeNextRetry(0));
        }
    }

    private ChannelMessage buildMessage(Notification notification) {
        return new ChannelMessage(
            notification.getId(),
            notification.getTitle(),
            notification.getAiSummary() != null
                ? notification.getAiSummary()
                : notification.getBody(),
            notification.getSource(),
            notification.getPriority(),
            notification.getExternalUrl(),
            notification.getCreatedAt()
        );
    }

    private Instant computeNextRetry(int attemptCount) {
        long minutes = switch (attemptCount) {
            case 0 -> 1;
            case 1 -> 5;
            case 2 -> 25;
            default -> throw new IllegalStateException("Max retries exceeded");
        };
        return Instant.now().plus(minutes, ChronoUnit.MINUTES);
    }
}
```

### 4-9. DigestChannelRouter (DIGEST 경로)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DigestChannelRouter {

    private final ChannelDeliveryLogRepository deliveryLogRepository;

    public void queue(Notification notification, NotificationChannel channel, RoutingRule rule) {
        // 해당 채널에 이미 활성 DIGEST 윈도우가 있는지 확인
        boolean windowExists = deliveryLogRepository
            .existsByChannelIdAndStatus(channel.getId(), DeliveryStatus.DIGEST_PENDING);

        Instant nextDeliveryAt = windowExists
            ? deliveryLogRepository
                .findMinNextRetryAtByChannelIdAndStatus(channel.getId(), DeliveryStatus.DIGEST_PENDING)
                .orElse(Instant.now().plus(rule.getDigestIntervalMin(), ChronoUnit.MINUTES))
            : Instant.now().plus(rule.getDigestIntervalMin(), ChronoUnit.MINUTES);

        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(notification.getId())
            .channelId(channel.getId())
            .status(DeliveryStatus.DIGEST_PENDING)
            .nextRetryAt(nextDeliveryAt)
            .build();

        deliveryLogRepository.save(log);
        log.info("event=digest_queued notification_id={} channel_id={} next_delivery_at={}",
            notification.getId(), channel.getId(), nextDeliveryAt);
    }
}
```

### 4-10. ChannelDeliveryScheduler (RETRY 백오프)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelDeliveryScheduler {

    private final ChannelDeliveryLogRepository deliveryLogRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelRepository channelRepository;
    private final ChannelProviderRegistry providerRegistry;
    private final CredentialEncryptionService encryptionService;

    @Scheduled(fixedDelay = 5 * 60 * 1000)  // 5분
    @Transactional
    public void processRetries() {
        List<ChannelDeliveryLog> retryLogs = deliveryLogRepository
            .findTop50ByStatusAndNextRetryAtBefore(DeliveryStatus.RETRY, Instant.now());

        retryLogs.forEach(this::retryDelivery);
    }

    private void retryDelivery(ChannelDeliveryLog deliveryLog) {
        // 재시도 로직: attemptCount 확인 → 3회 초과 시 DEAD
        // Provider.deliver() 호출 → 성공/실패 상태 업데이트
    }
}
```

### 4-11. NotificationDigestScheduler (Digest 윈도우 처리)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDigestScheduler {

    private final ChannelDeliveryLogRepository deliveryLogRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelRepository channelRepository;
    private final ChannelProviderRegistry providerRegistry;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final ChannelDeliveryMetrics metrics;

    @Scheduled(fixedDelay = 60 * 1000)  // 1분
    @Transactional
    public void processDigests() {
        // 만료된 DIGEST 윈도우: channel_id별로 그룹화
        List<Long> channelIds = deliveryLogRepository
            .findDistinctChannelIdsByStatusAndNextRetryAtBefore(
                DeliveryStatus.DIGEST_PENDING, Instant.now());

        channelIds.forEach(this::processDigestForChannel);
    }

    private void processDigestForChannel(Long channelId) {
        List<ChannelDeliveryLog> pendingLogs = deliveryLogRepository
            .findByChannelIdAndStatusAndNextRetryAtBefore(
                channelId, DeliveryStatus.DIGEST_PENDING, Instant.now());

        if (pendingLogs.isEmpty()) return;

        List<Long> notificationIds = pendingLogs.stream()
            .map(ChannelDeliveryLog::getNotificationId).toList();
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);

        NotificationChannel channel = channelRepository.findById(channelId)
            .orElseThrow();

        try {
            // 복수 알림 → LLM 묶음 요약
            LlmPrompt prompt = promptBuilder.buildDigestSummaryPrompt(notifications);
            String digestContent = llmProvider.chat(prompt);

            ChannelMessage digestMessage = ChannelMessage.builder()
                .notificationId(notifications.get(0).getId())
                .title("[묶음 알림] " + notifications.size() + "개")
                .body(digestContent)
                .source(notifications.get(0).getSource())
                .priority(NotificationPriority.MEDIUM)
                .notifiedAt(Instant.now())
                .build();

            NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
            ChannelDeliveryResult result = provider.deliver(channel, digestMessage);

            if (result.success()) {
                pendingLogs.forEach(l -> {
                    l.setStatus(DeliveryStatus.SUCCESS);
                    l.setDeliveredAt(Instant.now());
                    l.setExternalMessageId(result.externalMessageId());
                });
                channel.recordSuccess();
                metrics.recordDigestDelivery(channel.getChannelType().name(), notifications.size());
                log.info("event=digest_delivered channel_id={} count={}", channelId, notifications.size());
            } else {
                pendingLogs.forEach(l -> {
                    l.setStatus(result.retryable() ? DeliveryStatus.RETRY : DeliveryStatus.DEAD);
                    l.setLastError(result.errorMessage());
                });
            }
            deliveryLogRepository.saveAll(pendingLogs);

        } catch (Exception e) {
            log.error("event=digest_processing_failed channel_id={}", channelId, e);
        }
    }
}
```

---

## Phase 5: REST API 구현

### 5-1. NotificationChannelController

```
POST   /api/v1/channels              채널 생성 (검증 → 저장)
GET    /api/v1/channels              채널 목록
GET    /api/v1/channels/{id}         채널 상세
PUT    /api/v1/channels/{id}         채널 수정
DELETE /api/v1/channels/{id}         채널 삭제 (soft delete)
PATCH  /api/v1/channels/{id}/pause   채널 일시중지
PATCH  /api/v1/channels/{id}/resume  채널 재개
POST   /api/v1/channels/{id}/test    테스트 전송 (검증 API 호출)
```

**CreateChannelRequest:**
```java
public record CreateChannelRequest(
    @NotBlank String displayName,
    @NotNull ChannelType channelType,
    @NotBlank String credentialPlaintext,  // 저장 전 검증 후 암호화
    String targetIdentifier                // Slack/Telegram만 필요
) {}
```

**ChannelResponse** (credential 미포함):
```java
public record ChannelResponse(
    Long id,
    ChannelType channelType,
    String displayName,
    String keyPreview,         // Bot Token 마지막 4자리
    ChannelStatus status,
    int errorCount,
    String lastError,
    Instant lastDeliveredAt
) {}
```

### 5-2. RoutingRuleController

```
POST   /api/v1/routing-rules              규칙 생성
GET    /api/v1/routing-rules              규칙 목록 (priority_order 순)
PUT    /api/v1/routing-rules/{id}         규칙 수정
DELETE /api/v1/routing-rules/{id}         규칙 삭제
PATCH  /api/v1/routing-rules/reorder      순서 변경 (drag-to-reorder)
```

**CreateRoutingRuleRequest:**
```java
public record CreateRoutingRuleRequest(
    @NotBlank String ruleName,
    int priorityOrder,
    RoutingConditionDto conditions,
    @NotEmpty List<Long> channelIds,
    boolean stopOnMatch,
    @NotNull DeliveryMode deliveryMode,
    Integer digestIntervalMin            // deliveryMode=DIGEST 시 필수
) {}
```

**RoutingRuleResponse:**
```java
public record RoutingRuleResponse(
    Long id,
    String ruleName,
    int priorityOrder,
    RoutingConditionDto conditions,
    List<Long> channelIds,
    boolean stopOnMatch,
    boolean isEnabled,
    DeliveryMode deliveryMode,
    Integer digestIntervalMin
) {}
```

### 5-3. DeliveryFeedController

```
GET /api/v1/channels/delivery-feed?page=0&size=20&channelType=SLACK
```

```java
@GetMapping("/delivery-feed")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<Page<DeliveryFeedItem>>> getDeliveryFeed(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) ChannelType channelType) {

    Pageable pageable = PageRequest.of(page, Math.min(size, 50),
        Sort.by("deliveredAt").descending());
    Page<DeliveryFeedItem> feed =
        deliveryFeedService.getFeed(user.getId(), channelType, pageable);
    return ResponseEntity.ok(ApiResponse.success(feed));
}
```

**DeliveryFeedItem:**
```java
public record DeliveryFeedItem(
    Long deliveryLogId,
    Long notificationId,
    String notificationTitle,
    Long channelId,
    ChannelType channelType,
    String channelDisplayName,
    String deliveredContent,       // COALESCE(n.ai_summary, n.body)
    Instant deliveredAt,
    DeliveryStatus status,
    String externalMessageId
) {}
```

**JPQL 쿼리** (DeliveryFeedRepository):
```java
@Query("""
    SELECT new com.notio.channel.dto.DeliveryFeedItem(
        cdl.id, n.id, n.title, nc.id, nc.channelType, nc.displayName,
        COALESCE(n.aiSummary, n.body), cdl.deliveredAt, cdl.status, cdl.externalMessageId
    )
    FROM ChannelDeliveryLog cdl
    JOIN Notification n ON n.id = cdl.notificationId
    JOIN NotificationChannel nc ON nc.id = cdl.channelId
    WHERE n.userId = :userId
      AND cdl.status = 'SUCCESS'
      AND (:channelType IS NULL OR nc.channelType = :channelType)
    ORDER BY cdl.deliveredAt DESC
    """)
Page<DeliveryFeedItem> findFeed(
    @Param("userId") Long userId,
    @Param("channelType") ChannelType channelType,
    Pageable pageable);
```

---

## Phase 6: 공통 설정

### 6-1. SecurityConfig 변경

- `/api/v1/chat/**` → 제거
- `/api/v1/channels/**` → 인증 필요
- `/api/v1/routing-rules/**` → 인증 필요

### 6-2. ErrorCode 추가

```java
CHANNEL_NOT_FOUND("CHANNEL_NOT_FOUND", "채널을 찾을 수 없습니다.")
CHANNEL_CREDENTIAL_INVALID("CHANNEL_CREDENTIAL_INVALID", "채널 자격증명이 유효하지 않습니다.")
ROUTING_RULE_NOT_FOUND("ROUTING_RULE_NOT_FOUND", "라우팅 규칙을 찾을 수 없습니다.")
DIGEST_INTERVAL_REQUIRED("DIGEST_INTERVAL_REQUIRED", "묶음 전송 시 간격 설정이 필요합니다.")
```

---

## 구현 순서

| 순서 | 작업 |
|------|------|
| 1 | `push/` 패키지 삭제, `FirebaseConfig` 삭제, `build.gradle` Firebase 제거 |
| 2 | V12, V13 마이그레이션 SQL 작성 |
| 3 | `channel/domain/` 엔티티 4개 + enum 5개 구현 |
| 4 | `channel/provider/` — 인터페이스 + 3개 Provider 구현 |
| 5 | `ChannelProviderRegistry`, `RoutingRuleEvaluator` 구현 |
| 6 | `ChannelRouter` (IMMEDIATE), `DigestChannelRouter` (DIGEST) 구현 |
| 7 | `ChannelDeliveryScheduler` (RETRY), `NotificationDigestScheduler` (Digest) 구현 |
| 8 | `NotificationChannelService`, `RoutingRuleService` 구현 |
| 9 | `NotificationChannelController`, `RoutingRuleController` 구현 |
| 10 | `DeliveryFeedController` + `DeliveryFeedItem` 구현 |
| 11 | `NotificationService` — PushService 제거, 요약+라우팅 비동기 오케스트레이션 |
| 12 | V14 마이그레이션 SQL 작성 (plan_ai.md Phase 3 완료 후) |
| 13 | `SecurityConfig` 업데이트 |

---

## 테스트 계획

### 단위 테스트

| 테스트 | 검증 항목 |
|--------|---------|
| `RoutingRuleEvaluatorTest` | source/priority 조건 AND 매칭, null=전체 매칭 |
| `ChannelRouterTest` | IMMEDIATE/DIGEST 분기, stop_on_match, 비활성 채널 skip |
| `DigestChannelRouterTest` | 윈도우 신규 생성, 기존 윈도우 재사용 |
| `SlackBlockKitFormatterTest` | 우선순위별 색상, 특수문자 처리 |
| `TelegramMarkdownFormatterTest` | MarkdownV2 이스케이프 |
| `DiscordEmbedFormatterTest` | 색상 십진수 매핑 |

### 통합 테스트 (Testcontainers)

| 테스트 | 검증 항목 |
|--------|---------|
| `ChannelDeliverySchedulerTest` | RETRY 백오프 3회 후 DEAD 전환 |
| `NotificationDigestSchedulerTest` | DIGEST_PENDING → 만료 → LLM 묶음 요약 → SUCCESS |
| `DeliveryFeedControllerTest` | `@WebMvcTest` — 페이지네이션, channelType 필터 |
| `V14MigrationTest` | Flyway 마이그레이션 후 스키마 검증 |
