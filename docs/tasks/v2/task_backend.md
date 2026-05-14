# Task: Backend 개발 체크리스트 (AI / LLM 요약 파이프라인)

> **대상 버전**: v2.1
> **작성일**: 2026-05-14
> **연관 Plan**: `docs/plans/v2/plan_ai.md`

---

## Phase 1: chat/ 패키지 제거 및 의존성 해소

### 1-1. LlmMetrics 신규 생성

**파일**: `ai/metrics/LlmMetrics.java`

- [x] `@Component`, `@RequiredArgsConstructor` 선언
- [x] `NotioMetrics` 주입
- [x] `recordLlmCall(String outcome, Duration duration)` 메서드 구현
  - [x] `notio_llm_call_total` 카운터 (`outcome` 태그)
  - [x] `notio_llm_call_duration` 타이머

---

### 1-2. OllamaLlmProvider — ChatMetrics → LlmMetrics 교체

**파일**: `ai/llm/OllamaLlmProvider.java`

- [x] `ChatMetrics` 의존성 제거, `LlmMetrics` 주입으로 교체
- [x] 스트리밍 전용 메트릭 호출 제거
  - [x] `recordFirstChunk` 호출 제거
  - [x] `incrementActiveStreams` 호출 제거
  - [x] `decrementActiveStreams` 호출 제거
- [x] `recordLlmCall(outcome, duration)` 호출로 교체

---

### 1-3. PgvectorRagRetriever — ChatMetrics → NotificationFlowMetrics 교체

**파일**: `ai/rag/PgvectorRagRetriever.java`

- [x] `ChatMetrics` 의존성 제거, `NotificationFlowMetrics` 주입으로 교체
- [x] 기존 ChatMetrics 메트릭 호출을 `recordRagRetrieval(boolean, Duration)` 호출로 교체

---

### 1-4. chat/ 패키지 전체 삭제

> **주의**: 1-2, 1-3 완료 후 진행

- [x] `chat/controller/ChatController.java` 삭제
- [x] `chat/domain/ChatMessage.java` 삭제
- [x] `chat/domain/ChatMessageRole.java` 삭제
- [x] `chat/dto/ChatMessageResponse.java` 삭제
- [x] `chat/dto/ChatRequest.java` 삭제
- [x] `chat/dto/DailySummaryResponse.java` 삭제
- [x] `chat/metrics/ChatMetrics.java` 삭제
- [x] `chat/repository/ChatMessageRepository.java` 삭제
- [x] `chat/service/ChatPromptContext.java` 삭제
- [x] `chat/service/ChatService.java` 삭제
- [x] `chat/service/ChatTimeRangeExtractor.java` 삭제
- [x] `chat/service/DailySummaryService.java` 삭제
- [x] 삭제 후 `./gradlew compileJava` 통과 확인

---

## Phase 2: PromptBuilder 수정

**파일**: `ai/prompt/PromptBuilder.java`

### 2-1. 기존 메서드 제거

- [x] `buildChatPrompt(List<RagDocument>, List<ChatMessage>, String)` 제거
- [x] `buildDailySummaryPrompt(LocalDate, List<Notification>)` 제거
- [x] `formatRecentMessages(List<ChatMessage>)` 제거
- [x] `ChatMessage` import 제거

### 2-2. buildNotificationSummaryPrompt 추가

- [x] `buildNotificationSummaryPrompt(Notification, List<RagDocument>)` 메서드 구현
  - [x] 시스템 프롬프트: 2~4문장, 마크다운, 유사 알림 언급, 500자 제한 규칙 포함
  - [x] 유저 프롬프트: `소스`, `제목`, `우선순위`, `내용`, `링크(nullable)` 포함
  - [x] RAG context 존재 시 유사 과거 알림 최대 3개 (`similarityScore` 포함) 추가
  - [x] `LlmPrompt.of(systemPrompt, userPrompt)` 반환

### 2-3. buildDigestSummaryPrompt 추가

- [x] `buildDigestSummaryPrompt(List<Notification>)` 메서드 구현
  - [x] 시스템 프롬프트: 전체 요약 1~2문장, 목록형 1줄 요약, 1000자 제한 규칙 포함
  - [x] 유저 프롬프트: 알림 수, 각 알림의 소스/제목/우선순위/내용 포함
  - [x] `aiSummary != null`이면 `aiSummary`, 아니면 `body` 사용
  - [x] `truncate(String, int)` private 헬퍼 메서드 추가 (maxLen=300)
  - [x] `LlmPrompt.of(systemPrompt, userPrompt)` 반환

---

## Phase 3: NotificationSummaryService 구현

### 3-1. Notification 엔티티 변경

**파일**: `notification/domain/Notification.java`

- [x] `ai_summary` 컬럼 필드 추가: `@Column(name = "ai_summary", columnDefinition = "TEXT")`, nullable
- [x] Flyway 마이그레이션 스크립트 작성: `ALTER TABLE notifications ADD COLUMN ai_summary TEXT`

### 3-2. NotificationRepository 추가

**파일**: `notification/repository/NotificationRepository.java`

- [x] `updateAiSummary(@Param("id") Long id, @Param("summary") String summary)` 추가
  - [x] `@Modifying`, `@Query("UPDATE Notification n SET n.aiSummary = :summary WHERE n.id = :id")` 선언

### 3-3. NotificationSummaryService 구현

**파일**: `notification/service/NotificationSummaryService.java`

- [x] `@Slf4j`, `@Service`, `@RequiredArgsConstructor` 선언
- [x] 의존성 주입: `RagRetriever`, `PromptBuilder`, `LlmProvider`, `NotificationRepository`, `NotioAiProperties`, `NotificationFlowMetrics`
- [x] `summarize(Notification)` 메서드 구현
  - [x] `shouldSummarize()` false 시 debug 로그 출력 후 `null` 반환
  - [x] RAG 컨텍스트 조회: `ragRetriever.retrieve(userId, title + " " + body, Optional.empty())`
  - [x] 프롬프트 생성 후 `llmProvider.chat(prompt)` 호출
  - [x] `notificationRepository.updateAiSummary(id, summary)` 저장
  - [x] `metrics.recordAiSummarization("success", duration)` 기록
  - [x] 성공 로그: `event=ai_summarization_success notification_id={} source={} summary_len={}`
  - [x] 실패 시 `metrics.recordAiSummarization("failure", duration)` 기록 후 `null` 반환
  - [x] 실패 로그: `event=ai_summarization_failed notification_id={} source={} error={}`
- [x] `shouldSummarize(Notification)` private 메서드 구현
  - [x] `aiProperties.summarizeSources()` null/empty 시 `true` 반환
  - [x] 소스 이름 포함 여부로 반환

---

## Phase 4: AnthropicLlmProvider 구현

**파일**: `ai/llm/AnthropicLlmProvider.java`

- [x] `@Slf4j`, `@Service`, `@ConditionalOnProperty(name = "notio.ai.provider", havingValue = "anthropic")`, `@RequiredArgsConstructor` 선언
- [x] 의존성 주입: `ChatModel anthropicChatModel`, `NotioAiProperties`, `LlmMetrics`
- [x] `LlmProvider` 인터페이스 구현
- [x] `chat(LlmPrompt)` 메서드 구현
  - [x] `SystemMessage`, `UserMessage` 조합으로 `Prompt` 생성
  - [x] `anthropicChatModel.call(prompt)` 호출
  - [x] `response.getResult().getOutput().getText()` 추출
  - [x] `llmMetrics.recordLlmCall("success", duration)` 기록
  - [x] 실패 시 `llmMetrics.recordLlmCall("failure", duration)` 기록 후 `AiException` throw
- [x] `stream(LlmPrompt, Consumer<String>)` 메서드 stub 구현 (`UnsupportedOperationException`)

---

## Phase 5: NotioAiProperties 변경

**파일**: `ai/config/NotioAiProperties.java`

- [x] `@ConfigurationProperties(prefix = "notio.ai")` record로 변경
- [x] `provider` 필드 추가 (`@DefaultValue("ollama")`)
- [x] `llmTimeout` 필드 유지
- [x] `embeddingTimeout` 필드 유지
- [x] `summarizeSources` 필드 추가 (`@DefaultValue("CLAUDE,CODEX")`, `List<String>`)

---

## Phase 6: NotificationFlowMetrics 및 설정 추가

### 6-1. NotificationFlowMetrics 메서드 추가

**파일**: `notification/metrics/NotificationFlowMetrics.java`

- [x] `recordAiSummarization(String outcome, Duration duration)` 추가
  - [x] `notio_ai_summarization_total` 카운터 (`outcome` 태그)
  - [x] `notio_ai_summarization_duration` 타이머
- [x] `recordRagRetrieval(boolean timeRangeApplied, Duration duration)` 추가
  - [x] `notio_rag_retrieval_total` 카운터 (`time_range_applied` 태그)
  - [x] `notio_rag_retrieval_duration` 타이머

### 6-2. application.yml 설정 추가

**파일**: `src/main/resources/application.yml`

- [x] `notio.ai.provider: ${NOTIO_AI_PROVIDER:ollama}` 추가
- [x] `notio.ai.summarize-sources: ${NOTIO_AI_SUMMARIZE_SOURCES:CLAUDE,CODEX}` 추가
- [x] `notio.ai.llm-timeout: ${NOTIO_AI_LLM_TIMEOUT:20s}` 추가
- [x] `spring.ai.anthropic.api-key: ${ANTHROPIC_API_KEY:}` 추가
- [x] `spring.ai.anthropic.chat.options.model: claude-haiku-4-5` 추가
- [x] `spring.ai.anthropic.chat.options.max-tokens: 1024` 추가

---

## Phase 7: 테스트

### 단위 테스트

- [ ] `NotificationSummaryServiceTest`
  - [ ] `shouldSummarize()` — CLAUDE/CODEX 소스 포함 시 요약 실행
  - [ ] `shouldSummarize()` — GITHUB 소스(미포함) 시 skip, null 반환
  - [ ] `shouldSummarize()` — `summarizeSources` null/empty 시 항상 실행
  - [ ] LLM 호출 실패 시 null 반환 (예외 미전파 확인)
- [ ] `PromptBuilderTest`
  - [ ] `buildNotificationSummaryPrompt` — RAG context 없을 때 프롬프트 구조
  - [ ] `buildNotificationSummaryPrompt` — RAG context 있을 때 유사 알림 3개 포함 확인
  - [ ] `buildDigestSummaryPrompt` — 복수 알림 → 목록 형식 포함 확인
  - [ ] `buildDigestSummaryPrompt` — aiSummary 있는 알림은 aiSummary 사용, 없으면 body 사용

### 통합 테스트

- [ ] `NotificationSummaryServiceIntegrationTest`
  - [ ] CLAUDE 소스 알림 → `ai_summary` DB 저장 확인 (Testcontainers)
  - [ ] GITHUB 소스 알림 → 요약 skip 확인 (기본 설정 CLAUDE,CODEX만)
- [ ] `OllamaLlmProviderTest`
  - [ ] LLM 타임아웃 시 `AiException` 발생 확인
