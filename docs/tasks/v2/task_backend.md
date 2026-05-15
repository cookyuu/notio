# Task: Backend 개발 체크리스트 (AI/LLM 요약 파이프라인)

> **대상 버전**: v2.1
> **작성일**: 2026-05-15
> **연관 Plan**: `docs/plans/v2/plan_ai.md`

---

## Phase 1: chat/ 패키지 제거 및 의존성 해소

### 1-1. LlmMetrics 신규 생성

- [x] `ai/metrics/LlmMetrics.java` 파일 생성
  - [x] `@Component`, `@RequiredArgsConstructor` 적용
  - [x] `NotioMetrics` 의존성 주입
  - [x] `recordLlmCall(String outcome, Duration duration)` 메서드 구현
    - [x] `notio_llm_call_total` 카운터 (`outcome` 태그)
    - [x] `notio_llm_call_duration` 타이머

### 1-2. NotificationFlowMetrics 메서드 추가

- [x] `recordAiSummarization(String outcome, Duration duration)` 메서드 추가
  - [x] `notio_ai_summarization_total` 카운터 (`outcome` 태그)
  - [x] `notio_ai_summarization_duration` 타이머
- [x] `recordRagRetrieval(boolean timeRangeApplied, Duration duration)` 메서드 추가
  - [x] `notio_rag_retrieval_total` 카운터 (`time_range_applied` 태그)
  - [x] `notio_rag_retrieval_duration` 타이머

### 1-3. OllamaLlmProvider 수정

- [x] `ChatMetrics` 의존성 → `LlmMetrics`로 교체
- [x] 스트리밍 전용 메트릭 제거
  - [x] `recordFirstChunk` 호출 제거
  - [x] `incrementActiveStreams` 호출 제거
  - [x] `decrementActiveStreams` 호출 제거
- [x] `recordLlmCall(String mode, String outcome, Duration duration)` 호출을 `recordLlmCall(String outcome, Duration duration)`으로 변경

### 1-4. PgvectorRagRetriever 수정

- [x] `ChatMetrics` 의존성 → `NotificationFlowMetrics`로 교체
- [x] RAG 검색 메트릭 호출을 `recordRagRetrieval(boolean, Duration)`으로 변경

### 1-5. chat/ 패키지 전체 삭제

> **주의**: 1-3, 1-4 완료 후 삭제

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
- [x] 컴파일 오류 0개 확인 (`./gradlew compileJava`)

---

## Phase 2: PromptBuilder 수정

**파일**: `ai/prompt/PromptBuilder.java`

### 2-1. 기존 메서드 제거

- [x] `buildChatPrompt(List<RagDocument>, List<ChatMessage>, String)` 제거
- [x] `buildDailySummaryPrompt(LocalDate, List<Notification>)` 제거
- [x] `formatRecentMessages(List<ChatMessage>)` 제거
- [x] `ChatMessage` import 제거

### 2-2. buildNotificationSummaryPrompt 추가

- [x] `buildNotificationSummaryPrompt(Notification, List<RagDocument>)` 메서드 추가
  - [x] 시스템 프롬프트: 2~4문장, 500자 제한, 마크다운 활용 규칙 포함
  - [x] 유저 프롬프트: 소스, 제목, 우선순위, 내용, 링크(nullable) 포함
  - [x] RAG context가 있을 경우 유사 과거 알림 top-3 포함
  - [x] `LlmPrompt.of(systemPrompt, userPrompt)` 반환

### 2-3. buildDigestSummaryPrompt 추가

- [x] `buildDigestSummaryPrompt(List<Notification>)` 메서드 추가
  - [x] 시스템 프롬프트: 첫 줄 전체 요약, 항목별 1줄 요약, 1000자 제한 규칙 포함
  - [x] 유저 프롬프트: 알림 개수 표시, 각 알림 소스/제목/우선순위/내용 포함
  - [x] `n.getAiSummary() != null` 이면 aiSummary, 아니면 body 사용
  - [x] `truncate(String, int)` private 헬퍼 메서드 추가 (최대 300자)
  - [x] `LlmPrompt.of(systemPrompt, userPrompt)` 반환

---

## Phase 3: NotificationSummaryService 구현

### 3-1. Notification 엔티티 변경

**파일**: `notification/domain/Notification.java`

- [x] `aiSummary` 필드 추가
  - [x] `@Column(name = "ai_summary", columnDefinition = "TEXT")` 적용
  - [x] nullable (LLM 요약 없을 경우 null 유지)

### 3-2. NotificationRepository 메서드 추가

**파일**: `notification/repository/NotificationRepository.java`

- [x] `updateAiSummary(@Param("id") Long id, @Param("summary") String summary)` 추가
  - [x] `@Modifying` 적용
  - [x] `@Query("UPDATE Notification n SET n.aiSummary = :summary WHERE n.id = :id")` 적용

### 3-3. NotificationSummaryService 구현

**파일**: `notification/service/NotificationSummaryService.java`

- [x] `@Slf4j`, `@Service`, `@RequiredArgsConstructor` 적용
- [x] 의존성 주입
  - [x] `RagRetriever ragRetriever`
  - [x] `PromptBuilder promptBuilder`
  - [x] `LlmProvider llmProvider`
  - [x] `NotificationRepository notificationRepository`
  - [x] `NotioAiProperties aiProperties`
  - [x] `NotificationFlowMetrics metrics`
- [x] `summarize(Notification notification)` 메서드 구현
  - [x] `shouldSummarize()` 체크 → false 시 null 반환 + debug 로그
  - [x] `ragRetriever.retrieve()` 호출 (userId, title+body 합성 쿼리, Optional.empty())
  - [x] `promptBuilder.buildNotificationSummaryPrompt()` 호출
  - [x] `llmProvider.chat()` 호출
  - [x] `notificationRepository.updateAiSummary()` 호출
  - [x] `metrics.recordAiSummarization("success", duration)` 호출
  - [x] info 로그: notification_id, source, summary_len
  - [x] 예외 발생 시 `metrics.recordAiSummarization("failure", duration)` 호출
  - [x] 예외 발생 시 warn 로그 후 null 반환
- [x] `shouldSummarize(Notification)` private 메서드 구현
  - [x] `aiProperties.summarizeSources()` null/empty 시 true 반환
  - [x] `configured.contains(notification.getSource().name())` 반환

---

## Phase 4: AnthropicLlmProvider 구현

**파일**: `ai/llm/AnthropicLlmProvider.java`

- [ ] `@Slf4j`, `@Service`, `@RequiredArgsConstructor` 적용
- [ ] `@ConditionalOnProperty(name = "notio.ai.provider", havingValue = "anthropic")` 적용
- [ ] `LlmProvider` 인터페이스 구현
- [ ] 의존성 주입
  - [ ] `ChatModel anthropicChatModel`
  - [ ] `NotioAiProperties aiProperties`
  - [ ] `LlmMetrics llmMetrics`
- [ ] `chat(LlmPrompt prompt)` 메서드 구현
  - [ ] `SystemMessage` + `UserMessage` 리스트로 `Prompt` 생성
  - [ ] `anthropicChatModel.call()` 호출
  - [ ] `response.getResult().getOutput().getText()` 추출
  - [ ] `llmMetrics.recordLlmCall("success", duration)` 호출
  - [ ] 예외 발생 시 `llmMetrics.recordLlmCall("failure", duration)` 후 `AiException` 래핑
- [ ] `stream(LlmPrompt, Consumer<String>)` 메서드 구현
  - [ ] `UnsupportedOperationException` throw (요약 파이프라인은 동기만 사용)

---

## Phase 5: NotioAiProperties 변경

**파일**: `ai/config/NotioAiProperties.java`

- [ ] `record` 타입 확인 (`@ConfigurationProperties(prefix = "notio.ai")`)
- [ ] `summarizeSources` 필드 추가
  - [ ] `@DefaultValue("CLAUDE,CODEX") List<String> summarizeSources`
- [ ] 기존 필드 유지: `provider`, `llmTimeout`, `embeddingTimeout`

---

## Phase 6: application.yml 설정 추가

**파일**: `src/main/resources/application.yml`

- [ ] `notio.ai.provider` 설정 추가 (`${NOTIO_AI_PROVIDER:ollama}`)
- [ ] `notio.ai.summarize-sources` 설정 추가 (`${NOTIO_AI_SUMMARIZE_SOURCES:CLAUDE,CODEX}`)
- [ ] `notio.ai.llm-timeout` 설정 추가 (`${NOTIO_AI_LLM_TIMEOUT:20s}`)
- [ ] `spring.ai.anthropic.api-key` 설정 추가 (`${ANTHROPIC_API_KEY:}`)
- [ ] `spring.ai.anthropic.chat.options.model` 설정 추가 (`claude-haiku-4-5`)
- [ ] `spring.ai.anthropic.chat.options.max-tokens` 설정 추가 (`1024`)

---

## Phase 7: 테스트 작성

### 단위 테스트

- [ ] `NotificationSummaryServiceTest`
  - [ ] `shouldSummarize()` — CLAUDE 소스 포함 시 true 반환
  - [ ] `shouldSummarize()` — GITHUB 소스 미포함 시 false 반환 (기본 설정 CLAUDE,CODEX)
  - [ ] `summarize()` — summarizeSources 미설정(null/empty) 시 모든 소스 요약
  - [ ] `summarize()` — LLM 예외 발생 시 null 반환, failure 메트릭 기록
- [ ] `PromptBuilderTest`
  - [ ] `buildNotificationSummaryPrompt()` — RAG context 포함 시 "유사 과거 알림" 섹션 존재
  - [ ] `buildNotificationSummaryPrompt()` — RAG context 미포함 시 "유사 과거 알림" 섹션 미존재
  - [ ] `buildDigestSummaryPrompt()` — 복수 알림 → 소스/제목 순서 정확한지 검증
  - [ ] `buildDigestSummaryPrompt()` — aiSummary 있을 경우 body 대신 aiSummary 사용

### 통합 테스트

- [ ] `NotificationSummaryServiceIntegrationTest`
  - [ ] CLAUDE 소스 알림 → `ai_summary` DB 저장 확인 (Testcontainers PostgreSQL)
  - [ ] GITHUB 소스 알림 → 요약 skip, `ai_summary` null 확인
- [ ] `OllamaLlmProviderTest`
  - [ ] LLM 타임아웃 시 `AiException` 발생 확인

---

## 최종 검증

- [ ] `./gradlew compileJava` — 컴파일 오류 0개
- [ ] `./gradlew test` — 전체 테스트 통과
- [ ] `chat/` 패키지 관련 참조 없음 (`grep -r "com.notio.chat"` 결과 0건)
- [ ] `notio_ai_summarization_total`, `notio_llm_call_total`, `notio_rag_retrieval_total` 메트릭 노출 확인 (`/actuator/metrics`)
