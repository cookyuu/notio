# Plan: AI / LLM 요약 파이프라인

> **대상 버전**: v2.1
> **작성일**: 2026-05-12
> **연관 Blueprint**: `docs/blueprint/notio_blueprint_v2.md` §7

---

## 개요

v2.0의 인터랙티브 AI 채팅을 제거하고 LLM+RAG를 **알림 요약 파이프라인 전용**으로 전환한다.
Codex/Claude Code 알림의 장문 출력을 채팅 플랫폼 전달에 적합한 짧은 메시지로 변환하는 것이 핵심 목표.

---

## Phase 1: chat/ 패키지 제거 및 의존성 해소

### 1-1. 삭제 대상 파일

```
backend/src/main/java/com/notio/chat/
├── controller/ChatController.java
├── domain/ChatMessage.java
├── domain/ChatMessageRole.java
├── dto/ChatMessageResponse.java
├── dto/ChatRequest.java
├── dto/DailySummaryResponse.java
├── metrics/ChatMetrics.java
├── repository/ChatMessageRepository.java
└── service/
    ├── ChatPromptContext.java
    ├── ChatService.java
    ├── ChatTimeRangeExtractor.java
    └── DailySummaryService.java
```

### 1-2. ChatMetrics 의존 해소 (컴파일 오류 방지)

`chat/` 삭제 전에 반드시 아래 두 파일에서 `ChatMetrics` 의존을 교체한다.

**`ai/rag/PgvectorRagRetriever.java`**
- `ChatMetrics` → `NotificationFlowMetrics`로 교체
- `NotificationFlowMetrics`에 추가할 메서드:
  ```java
  public void recordRagRetrieval(boolean timeRangeApplied, Duration duration) {
      notioMetrics.incrementCounter("notio_rag_retrieval_total",
          Tags.of("time_range_applied", String.valueOf(timeRangeApplied)));
      notioMetrics.recordTimer("notio_rag_retrieval_duration", Tags.empty(), duration);
  }
  ```

**`ai/llm/OllamaLlmProvider.java`**
- `ChatMetrics` → `ai/metrics/LlmMetrics` (신규) 로 교체
- 스트리밍 전용 메트릭 제거 (`recordFirstChunk`, `incrementActiveStreams`, `decrementActiveStreams`)
- 유지할 메서드: `recordLlmCall(String mode, String outcome, Duration duration)`

### 1-3. LlmMetrics 신규 생성

```
ai/metrics/LlmMetrics.java
```

```java
@Component
@RequiredArgsConstructor
public class LlmMetrics {
    private final NotioMetrics notioMetrics;

    public void recordLlmCall(String outcome, Duration duration) {
        notioMetrics.incrementCounter("notio_llm_call_total",
            Tags.of("outcome", outcome));
        notioMetrics.recordTimer("notio_llm_call_duration", Tags.empty(), duration);
    }
}
```

---

## Phase 2: PromptBuilder 수정

**파일**: `ai/prompt/PromptBuilder.java`

### 2-1. 제거할 메서드
- `buildChatPrompt(List<RagDocument>, List<ChatMessage>, String)` — 채팅 응답 프롬프트
- `buildDailySummaryPrompt(LocalDate, List<Notification>)` — 일일 요약
- `formatRecentMessages(List<ChatMessage>)` — 채팅 이력 포맷
- `ChatMessage` import 제거

### 2-2. 추가할 메서드: `buildNotificationSummaryPrompt`

```java
/**
 * 단일 알림을 채팅 플랫폼 전달용으로 요약하는 프롬프트.
 * RAG context로 유사 과거 알림을 참고하여 중복/반복 여부를 판단한다.
 */
public LlmPrompt buildNotificationSummaryPrompt(
        Notification notification,
        List<RagDocument> ragContext) {

    String systemPrompt = """
        당신은 개발자 알림을 채팅 플랫폼(Slack/Telegram/Discord)용으로 간결하게 요약하는 AI입니다.
        규칙:
        1. 2~4문장으로 핵심 내용과 필요한 조치를 명확히 설명하세요.
        2. 마크다운 굵게(**)와 코드 인라인(`)을 적절히 활용하세요.
        3. 유사한 과거 알림이 있다면 "이전과 동일한 유형" 임을 언급하세요.
        4. 최대 500자를 초과하지 마세요.
        5. 불필요한 인사말, 부연 설명을 생략하세요.
        """;

    StringBuilder userPrompt = new StringBuilder();
    userPrompt.append("## 알림 정보\n");
    userPrompt.append("- **소스**: ").append(notification.getSource()).append("\n");
    userPrompt.append("- **제목**: ").append(notification.getTitle()).append("\n");
    userPrompt.append("- **우선순위**: ").append(notification.getPriority()).append("\n");
    userPrompt.append("- **내용**:\n").append(notification.getBody()).append("\n");
    if (notification.getExternalUrl() != null) {
        userPrompt.append("- **링크**: ").append(notification.getExternalUrl()).append("\n");
    }

    if (!ragContext.isEmpty()) {
        userPrompt.append("\n## 유사 과거 알림 (참고용)\n");
        ragContext.stream().limit(3).forEach(doc ->
            userPrompt.append("- [").append(doc.source()).append("] ")
                      .append(doc.title()).append(" (유사도: ")
                      .append(String.format("%.2f", doc.similarityScore())).append(")\n")
        );
    }

    userPrompt.append("\n위 알림을 채팅 플랫폼 전달용으로 요약해주세요.");

    return LlmPrompt.of(systemPrompt, userPrompt.toString());
}
```

### 2-3. 추가할 메서드: `buildDigestSummaryPrompt`

```java
/**
 * 복수 알림을 하나의 묶음 메시지로 요약하는 프롬프트.
 * NotificationDigestScheduler가 호출한다.
 */
public LlmPrompt buildDigestSummaryPrompt(List<Notification> notifications) {

    String systemPrompt = """
        당신은 여러 개발자 알림을 하나의 묶음 요약 메시지로 작성하는 AI입니다.
        규칙:
        1. 첫 줄: 전체 요약 1~2문장 (총 N개 알림, 주요 주제 포함).
        2. 이후: 각 알림을 1줄로 요약 (- [소스] 제목: 핵심 내용 형식).
        3. 마크다운 목록(-)을 사용하세요.
        4. 최대 1000자를 초과하지 마세요.
        """;

    StringBuilder userPrompt = new StringBuilder();
    userPrompt.append("## 묶음 전달할 알림 목록 (").append(notifications.size()).append("개)\n\n");

    notifications.forEach(n -> {
        userPrompt.append("### [").append(n.getSource()).append("] ")
                  .append(n.getTitle()).append("\n");
        userPrompt.append("- 우선순위: ").append(n.getPriority()).append("\n");
        String body = n.getAiSummary() != null ? n.getAiSummary() : n.getBody();
        userPrompt.append("- 내용: ").append(truncate(body, 300)).append("\n\n");
    });

    userPrompt.append("위 알림들을 하나의 묶음 요약 메시지로 작성해주세요.");

    return LlmPrompt.of(systemPrompt, userPrompt.toString());
}

private String truncate(String text, int maxLen) {
    return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
}
```

---

## Phase 3: NotificationSummaryService 구현

**파일**: `notification/service/NotificationSummaryService.java`

### 3-1. 전체 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSummaryService {

    private final RagRetriever ragRetriever;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final NotificationRepository notificationRepository;
    private final NotioAiProperties aiProperties;
    private final NotificationFlowMetrics metrics;

    @Nullable
    public String summarize(Notification notification) {
        if (!shouldSummarize(notification)) {
            log.debug("event=ai_summarization_skipped notification_id={} source={}",
                notification.getId(), notification.getSource());
            return null;
        }

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
            log.info("event=ai_summarization_success notification_id={} source={} summary_len={}",
                notification.getId(), notification.getSource(), summary.length());
            return summary;

        } catch (Exception e) {
            metrics.recordAiSummarization("failure", Duration.between(start, Instant.now()));
            log.warn("event=ai_summarization_failed notification_id={} source={} error={}",
                notification.getId(), notification.getSource(), e.getMessage(), e);
            return null;
        }
    }

    private boolean shouldSummarize(Notification notification) {
        List<String> configured = aiProperties.summarizeSources();
        if (configured == null || configured.isEmpty()) return true;
        return configured.contains(notification.getSource().name());
    }
}
```

### 3-2. NotificationRepository 추가 메서드

```java
// NotificationRepository.java
@Modifying
@Query("UPDATE Notification n SET n.aiSummary = :summary WHERE n.id = :id")
void updateAiSummary(@Param("id") Long id, @Param("summary") String summary);
```

### 3-3. Notification 엔티티 변경

```java
// Notification.java — 필드 추가
@Column(name = "ai_summary", columnDefinition = "TEXT")
private String aiSummary;  // nullable, LLM 요약본
```

---

## Phase 4: AnthropicLlmProvider 구현

**파일**: `ai/llm/AnthropicLlmProvider.java`

```java
@Slf4j
@Service
@ConditionalOnProperty(name = "notio.ai.provider", havingValue = "anthropic")
@RequiredArgsConstructor
public class AnthropicLlmProvider implements LlmProvider {

    private final ChatModel anthropicChatModel;  // Spring AI Anthropic ChatModel
    private final NotioAiProperties aiProperties;
    private final LlmMetrics llmMetrics;

    @Override
    public String chat(LlmPrompt prompt) {
        Instant start = Instant.now();
        try {
            ChatResponse response = anthropicChatModel.call(
                new Prompt(
                    List.of(
                        new SystemMessage(prompt.systemPrompt()),
                        new UserMessage(prompt.userPrompt())
                    )
                )
            );
            String result = response.getResult().getOutput().getText();
            llmMetrics.recordLlmCall("success", Duration.between(start, Instant.now()));
            return result;
        } catch (Exception e) {
            llmMetrics.recordLlmCall("failure", Duration.between(start, Instant.now()));
            throw new AiException("Anthropic LLM call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void stream(LlmPrompt prompt, Consumer<String> chunkConsumer) {
        // 요약 파이프라인은 동기 chat()만 사용. stream()은 향후 확장용 stub.
        throw new UnsupportedOperationException("Streaming not used in summary pipeline");
    }
}
```

**`application.yml` 추가 설정:**
```yaml
notio:
  ai:
    provider: ${NOTIO_AI_PROVIDER:ollama}
    summarize-sources: ${NOTIO_AI_SUMMARIZE_SOURCES:CLAUDE,CODEX}
    llm-timeout: ${NOTIO_AI_LLM_TIMEOUT:20s}

spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
      chat:
        options:
          model: claude-haiku-4-5
          max-tokens: 1024
```

---

## Phase 5: NotioAiProperties 변경

```java
@ConfigurationProperties(prefix = "notio.ai")
public record NotioAiProperties(
    @DefaultValue("ollama") String provider,
    Duration llmTimeout,
    Duration embeddingTimeout,
    @DefaultValue("CLAUDE,CODEX") List<String> summarizeSources
) {}
```

---

## Phase 6: NotificationFlowMetrics 추가 메서드

```java
// NotificationFlowMetrics.java 에 추가
public void recordAiSummarization(String outcome, Duration duration) {
    notioMetrics.incrementCounter("notio_ai_summarization_total",
        Tags.of("outcome", outcome));
    notioMetrics.recordTimer("notio_ai_summarization_duration",
        Tags.empty(), duration);
}

public void recordRagRetrieval(boolean timeRangeApplied, Duration duration) {
    notioMetrics.incrementCounter("notio_rag_retrieval_total",
        Tags.of("time_range_applied", String.valueOf(timeRangeApplied)));
    notioMetrics.recordTimer("notio_rag_retrieval_duration",
        Tags.empty(), duration);
}
```

---

## 구현 순서

| 순서 | 작업 | 의존 |
|------|------|------|
| 1 | `LlmMetrics` 신규 생성 | — |
| 2 | `NotificationFlowMetrics`에 `recordAiSummarization`, `recordRagRetrieval` 추가 | 1 |
| 3 | `OllamaLlmProvider` — `ChatMetrics` → `LlmMetrics` 교체, 스트리밍 메트릭 제거 | 1 |
| 4 | `PgvectorRagRetriever` — `ChatMetrics` → `NotificationFlowMetrics` 교체 | 2 |
| 5 | `chat/` 패키지 전체 삭제 | 3, 4 완료 후 |
| 6 | `NotioAiProperties`에 `summarizeSources` 추가 | — |
| 7 | `PromptBuilder` — chat/daily 메서드 제거, summary/digest 메서드 추가 | 5 |
| 8 | `Notification` 엔티티 `aiSummary` 필드 추가 | — |
| 9 | `NotificationRepository.updateAiSummary()` 추가 | 8 |
| 10 | `NotificationSummaryService` 구현 | 7, 9 |
| 11 | `AnthropicLlmProvider` 구현 | — |
| 12 | `application.yml` 설정 추가 | 6, 11 |

---

## 테스트 계획

### 단위 테스트

| 테스트 클래스 | 검증 항목 |
|-------------|---------|
| `NotificationSummaryServiceTest` | `shouldSummarize()` CLAUDE/CODEX 포함/미포함, LLM 실패 시 null 반환 |
| `PromptBuilderTest` | `buildNotificationSummaryPrompt` — RAG context 포함/미포함 프롬프트 구조 |
| `PromptBuilderTest` | `buildDigestSummaryPrompt` — 복수 알림 → 프롬프트 목록 구조 |

### 통합 테스트

| 테스트 | 검증 항목 |
|--------|---------|
| `NotificationSummaryServiceIntegrationTest` | CLAUDE 소스 알림 → `ai_summary` DB 저장 확인 (Testcontainers) |
| `NotificationSummaryServiceIntegrationTest` | GITHUB 소스 알림 → 요약 skip 확인 (기본 설정 CLAUDE,CODEX만) |
| `OllamaLlmProviderTest` | LLM 타임아웃 시 AiException 발생 확인 |

---

## 모니터링 메트릭

```
notio_ai_summarization_total{outcome="success|failure"}
notio_ai_summarization_duration_seconds
notio_rag_retrieval_total{time_range_applied="false|true"}
notio_rag_retrieval_duration_seconds
notio_llm_call_total{outcome="success|failure"}
notio_llm_call_duration_seconds
```

**Grafana 권장 패널:**
- AI 요약 성공률 (5분 윈도우)
- 요약 평균 처리 시간 (p50 / p95)
- RAG 검색 지연 시간
- LLM 실패 시 알림: `notio_ai_summarization_total{outcome="failure"}` > 10 (5분)
