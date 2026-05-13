# Task: Backend 개발 체크리스트

> **대상 버전**: v2.1
> **작성일**: 2026-05-12
> **연관 Plan**: `docs/plans/v2/plan_backend.md`, `docs/plans/v2/plan_ai.md`

---

## Phase 1: 패키지 및 의존성 제거

### 1-1. AI 메트릭 의존성 해소 (chat/ 삭제 전 필수)
- [x] `ai/metrics/LlmMetrics.java` 신규 생성
- [x] `NotificationFlowMetrics`에 `recordAiSummarization()` 메서드 추가
- [x] `NotificationFlowMetrics`에 `recordRagRetrieval()` 메서드 추가
- [x] `OllamaLlmProvider` — `ChatMetrics` → `LlmMetrics`로 교체
- [x] `OllamaLlmProvider` — 스트리밍 메트릭 제거 (`recordFirstChunk`, `incrementActiveStreams`, `decrementActiveStreams`)
- [x] `PgvectorRagRetriever` — `ChatMetrics` → `NotificationFlowMetrics`로 교체

### 1-2. `chat/` 패키지 삭제
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
- [x] `./gradlew compileJava` 오류 없음 확인

### 1-3. `push/` 패키지 삭제
- [x] `push/controller/DeviceController.java` 삭제
- [x] `push/domain/Device.java` 삭제
- [x] `push/domain/DevicePlatform.java` 삭제
- [x] `push/dto/DeviceResponse.java` 삭제
- [x] `push/dto/RegisterDeviceRequest.java` 삭제
- [x] `push/repository/DeviceRepository.java` 삭제
- [x] `push/service/DeviceService.java` 삭제
- [x] `push/service/PushService.java` 삭제
- [x] `common/config/FirebaseConfig.java` 삭제
- [x] `build.gradle.kts`에서 `firebase-admin:9.4.2` 의존성 제거

---

## Phase 2: DB 마이그레이션

### 2-1. V12 — push → channel 전환
- [x] `resources/db/migration/V12__migrate_push_to_channels.sql` 파일 생성
- [x] `devices` 테이블 → `devices_deprecated` rename + COMMENT 추가
- [x] `notification_channels` 테이블 생성 (channel_type CHECK, status CHECK 포함)
- [x] `idx_notification_channels_user_id` 부분 인덱스 생성 (`deleted_at IS NULL`)
- [x] `idx_notification_channels_status` 부분 인덱스 생성
- [x] `routing_rules` 테이블 생성 (JSONB conditions, channel_ids 포함)
- [x] `idx_routing_rules_user_priority` 부분 인덱스 생성

### 2-2. V13 — 채널 전달 로그
- [x] `resources/db/migration/V13__create_channel_delivery_logs.sql` 파일 생성
- [x] `channel_delivery_logs` 테이블 생성 (status CHECK: PENDING/SUCCESS/FAILED/RETRY/DEAD)
- [x] `idx_delivery_logs_notification_id` 인덱스 생성
- [x] `idx_delivery_logs_retry` 부분 인덱스 생성 (`status = 'RETRY'`)
- [x] `uq_delivery_logs_active` UNIQUE 인덱스 생성 (PENDING/RETRY 상태)

### 2-3. V14 — AI 요약 + Digest 모드 + chat_messages 폐기 (Phase 3 완료 후)
- [x] `resources/db/migration/V14__add_ai_summary_digest_mode.sql` 파일 생성
- [x] `notifications.ai_summary TEXT` 컬럼 추가 + COMMENT
- [x] `routing_rules.delivery_mode VARCHAR(20)`, `digest_interval_min INT` 컬럼 추가
- [x] `chk_delivery_mode` CHECK 제약 추가 (IMMEDIATE / DIGEST)
- [x] `channel_delivery_logs` CHECK 제약 삭제 후 `DIGEST_PENDING` 포함 재생성
- [x] `uq_delivery_logs_active` UNIQUE 인덱스 삭제 후 DIGEST_PENDING 포함 재생성
- [x] `idx_delivery_logs_digest_pending` 인덱스 생성
- [x] `idx_delivery_logs_delivered_at` 인덱스 생성
- [x] `chat_messages` → `chat_messages_deprecated` rename + COMMENT

---

## Phase 3: AI 파이프라인 구현

### 3-1. NotioAiProperties 변경
- [x] `summarizeSources` 필드 추가 (`@DefaultValue("CLAUDE,CODEX")`)
- [x] `application.yml`에 `notio.ai.provider`, `notio.ai.summarize-sources`, `notio.ai.llm-timeout` 설정 추가

### 3-2. PromptBuilder 수정
- [x] `buildChatPrompt()`, `buildDailySummaryPrompt()`, `formatRecentMessages()` 제거
- [x] `ChatMessage` import 제거
- [x] `buildNotificationSummaryPrompt(Notification, List<RagDocument>)` 구현
  - [x] 시스템 프롬프트: 2~4문장, 500자 제한, 마크다운 활용 규칙
  - [x] 유저 프롬프트: 소스/제목/우선순위/내용/링크 포맷
  - [x] RAG context 상위 3개 유사 알림 포함
- [x] `buildDigestSummaryPrompt(List<Notification>)` 구현
  - [x] 시스템 프롬프트: 첫 줄 요약 + 각 알림 1줄 목록, 1000자 제한
  - [x] `COALESCE(aiSummary, body)` 사용, 300자 truncate
- [x] `truncate(String, int)` private 유틸 메서드 추가

### 3-3. NotificationSummaryService 구현
- [x] `notification/service/NotificationSummaryService.java` 생성
- [x] `summarize(Notification)` — `@Nullable` 반환
- [x] `shouldSummarize()` — `summarizeSources` 기반 필터링
- [x] RAG 조회 → 프롬프트 빌드 → LLM 호출 → `notificationRepository.updateAiSummary()` 저장
- [x] 성공/실패 시 `metrics.recordAiSummarization()` 기록
- [x] 실패 시 null 반환 (예외 전파 없음)

### 3-4. Notification 엔티티 변경
- [x] `aiSummary` 필드 추가 (`@Column(name = "ai_summary", columnDefinition = "TEXT")`)
- [x] `NotificationDetailResponse`에 `aiSummary` 필드 추가

### 3-5. NotificationRepository 수정
- [x] `@Modifying @Query` — `updateAiSummary(@Param("id"), @Param("summary"))` 추가

### 3-6. AnthropicLlmProvider 구현
- [x] `ai/llm/AnthropicLlmProvider.java` 생성
- [x] `@ConditionalOnProperty(name = "notio.ai.provider", havingValue = "anthropic")` 적용
- [x] `chat(LlmPrompt)` — Spring AI `ChatModel.call()` 동기 구현
- [x] `stream()` — `UnsupportedOperationException` stub
- [x] `application.yml` — Spring AI Anthropic 설정 추가 (model: `claude-haiku-4-5`, max-tokens: 1024)

---

## Phase 4: channel/ 모듈 구현

### 4-1. 도메인 엔티티 및 Enum
- [x] `channel/domain/NotificationChannel.java` — `@SQLDelete`, `@SQLRestriction` soft delete 구현
  - [x] `recordSuccess()` — status ACTIVE, errorCount 0, lastDeliveredAt 갱신
  - [x] `recordFailure(String)` — errorCount 증가, 5회 이상 ERROR 전환
  - [x] `isDeliverable()`, `pause()`, `resume()` 구현
- [x] `channel/domain/RoutingRule.java` — JSONB 컨버터 포함 구현
- [x] `channel/domain/ChannelDeliveryLog.java` 구현
- [x] `channel/domain/ChannelType.java` Enum (SLACK, TELEGRAM, DISCORD)
- [x] `channel/domain/ChannelStatus.java` Enum (ACTIVE, PAUSED, ERROR)
- [x] `channel/domain/DeliveryMode.java` Enum (IMMEDIATE, DIGEST)
- [x] `channel/domain/DeliveryStatus.java` Enum (PENDING, SUCCESS, FAILED, RETRY, DEAD, DIGEST_PENDING)
- [x] `RoutingConditionConverter` (JSONB ↔ RoutingCondition) 구현
- [x] `LongListConverter` (JSONB ↔ List<Long>) 구현

### 4-2. Provider 인터페이스 및 Value Objects
- [x] `channel/provider/NotificationChannelProvider.java` 인터페이스 정의
- [x] `channel/provider/ChannelMessage.java` record 구현
- [x] `channel/provider/ChannelDeliveryResult.java` record + `success()`, `failure()` 정적 팩토리 구현
- [x] `channel/provider/ChannelValidationResult.java` record + `valid()`, `invalid()` 정적 팩토리 구현

### 4-3. SlackChannelProvider 구현
- [x] `channel/provider/SlackChannelProvider.java` 구현
- [x] `chat.postMessage` API 호출 + `ok` 필드 체크
- [x] HTTP 429 → `retryable=true`, 기타 에러 → `retryable=false`
- [x] `auth.test` API를 통한 `validate()` 구현
- [x] `SlackBlockKitFormatter` — 우선순위별 색상 Block Kit 포맷터 구현
  - [x] URGENT `#FF0000` / HIGH `#FF8C00` / MEDIUM `#4A90E2` / LOW `#9B9B9B`
  - [x] section(제목), section(본문), context(Source/Priority/링크) 블록

### 4-4. TelegramChannelProvider 구현
- [x] `channel/provider/TelegramChannelProvider.java` 구현
- [x] `sendMessage` API 호출 (`parse_mode: MarkdownV2`)
- [x] HTTP 429 → `retryable=true`, HTTP 400 → `retryable=false`
- [x] `getMe` API를 통한 `validate()` 구현
- [x] `TelegramMarkdownFormatter` — MarkdownV2 특수문자 이스케이프 유틸 구현 (`_ * [ ] ( ) ~ > # + - = | { } . !`)

### 4-5. DiscordChannelProvider 구현
- [x] `channel/provider/DiscordChannelProvider.java` 구현
- [x] `credential_encrypted` = Webhook URL 전체 저장 (`target_identifier` = null)
- [x] `POST {WEBHOOK_URL}?wait=true` → `message_id` 응답 수신
- [x] HTTP 429 → `retryable=true`, 400/401/404 → `retryable=false`, 5xx → `retryable=true`
- [x] `GET {WEBHOOK_URL}`로 `validate()` 구현
- [x] 우선순위별 색상 십진수 매핑 (URGENT: 16711680 / HIGH: 16744448 / MEDIUM: 4886754 / LOW: 10197915)

### 4-6. Registry 및 Evaluator
- [x] `channel/ChannelProviderRegistry.java` — `List<NotificationChannelProvider>` 자동 수집 Map 구현
- [x] `channel/RoutingRuleEvaluator.java` — source/priority AND 매칭, null/빈 목록 = 전체 매칭

### 4-7. ChannelRouter (IMMEDIATE)
- [x] `channel/ChannelRouter.java` 구현
- [x] priority_order 순 규칙 평가 루프
- [x] IMMEDIATE / DIGEST 분기 처리
- [x] `stop_on_match` 로직 구현
- [x] 비활성 채널 skip (`isDeliverable()`)
- [x] `deliverImmediate()` — 성공/실패/재시도 상태 저장 + 메트릭 기록
- [x] `computeNextRetry()` — 1분/5분/25분 백오프 구현
- [x] `buildMessage()` — `COALESCE(aiSummary, body)` 사용

### 4-8. DigestChannelRouter
- [x] `channel/DigestChannelRouter.java` 구현
- [x] 기존 DIGEST_PENDING 윈도우 있으면 기존 만료 시각 재사용, 없으면 신규 생성

### 4-9. ChannelDeliveryScheduler (RETRY 백오프)
- [x] `channel/ChannelDeliveryScheduler.java` 구현
- [x] `@Scheduled(fixedDelay = 5 * 60 * 1000)` — RETRY 항목 Top50 처리
- [x] attemptCount >= 3 시 DEAD 전환

### 4-10. NotificationDigestScheduler
- [x] `channel/NotificationDigestScheduler.java` 구현
- [x] `@Scheduled(fixedDelay = 60 * 1000)` — 만료된 DIGEST_PENDING 처리
- [x] channel_id별 그룹화 → `buildDigestSummaryPrompt()` → LLM 요약 → 채널 전달
- [x] 전달 성공 → DeliveryLog SUCCESS 일괄 업데이트 + `channel.recordSuccess()`
- [x] 전달 실패 → RETRY 또는 DEAD 상태 업데이트

### 4-11. NotificationService 수정
- [x] `PushService` 의존성 제거
- [x] `evictDailySummaryCache()` 메서드 및 호출 완전 제거
- [x] `NotificationSummaryService`, `ChannelRouter` 의존성 추가
- [x] Branch A: `notificationEmbeddingService.embedNotification()` 비동기 실행
- [x] Branch B: `summarize()` → `channelRouter.route()` 순차 비동기 실행 (A와 병렬)

### 4-12. Service 계층
- [x] `NotificationChannelService` — CRUD + pause/resume + test 전송 구현
- [x] `RoutingRuleService` — CRUD + `reorder(List<Long> orderedIds)` 구현

---

## Phase 5: REST API 구현

### 5-1. NotificationChannelController
- [ ] `POST /api/v1/channels` — 채널 생성 (Provider.validate() → 성공 시 암호화 저장)
- [ ] `GET /api/v1/channels` — 채널 목록
- [ ] `GET /api/v1/channels/{id}` — 채널 상세
- [ ] `PUT /api/v1/channels/{id}` — 채널 수정
- [ ] `DELETE /api/v1/channels/{id}` — soft delete
- [ ] `PATCH /api/v1/channels/{id}/pause` — 일시중지
- [ ] `PATCH /api/v1/channels/{id}/resume` — 재개
- [ ] `POST /api/v1/channels/{id}/test` — 테스트 전송
- [ ] `CreateChannelRequest` record 구현 (`@NotBlank displayName`, `@NotNull channelType`, `@NotBlank credentialPlaintext`)
- [ ] `ChannelResponse` record 구현 (credential 미포함, `keyPreview` = 마지막 4자리)

### 5-2. RoutingRuleController
- [ ] `POST /api/v1/routing-rules` — 규칙 생성
- [ ] `GET /api/v1/routing-rules` — 규칙 목록 (priority_order 오름차순)
- [ ] `PUT /api/v1/routing-rules/{id}` — 규칙 수정
- [ ] `DELETE /api/v1/routing-rules/{id}` — 규칙 삭제
- [ ] `PATCH /api/v1/routing-rules/reorder` — 순서 변경
- [ ] `CreateRoutingRuleRequest` record 구현 (DIGEST 시 `digestIntervalMin` 필수 검증)
- [ ] `RoutingRuleResponse` record 구현

### 5-3. DeliveryFeedController
- [ ] `GET /api/v1/channels/delivery-feed` — `page`, `size`, `channelType` 파라미터 처리
- [ ] `size` 최대 50 제한 (`Math.min(size, 50)`)
- [ ] `DeliveryFeedItem` record 구현 (`COALESCE(n.aiSummary, n.body)` 포함)
- [ ] `DeliveryFeedRepository` JPQL 쿼리 구현 (JOIN Notification + NotificationChannel, status='SUCCESS')

---

## Phase 6: 공통 설정

- [ ] `SecurityConfig` — `/api/v1/chat/**` 퍼밋 경로 제거
- [ ] `SecurityConfig` — `/api/v1/channels/**` 인증 필요 추가
- [ ] `SecurityConfig` — `/api/v1/routing-rules/**` 인증 필요 추가
- [ ] `ErrorCode` — `CHANNEL_NOT_FOUND` 추가
- [ ] `ErrorCode` — `CHANNEL_CREDENTIAL_INVALID` 추가
- [ ] `ErrorCode` — `ROUTING_RULE_NOT_FOUND` 추가
- [ ] `ErrorCode` — `DIGEST_INTERVAL_REQUIRED` 추가

---

## Phase 7: 테스트

### 단위 테스트
- [ ] `RoutingRuleEvaluatorTest` — source/priority AND 매칭, null=전체 매칭
- [ ] `ChannelRouterTest` — IMMEDIATE/DIGEST 분기, stop_on_match, 비활성 채널 skip
- [ ] `DigestChannelRouterTest` — 윈도우 신규 생성, 기존 윈도우 재사용
- [ ] `SlackBlockKitFormatterTest` — 우선순위별 색상, 특수문자 처리
- [ ] `TelegramMarkdownFormatterTest` — MarkdownV2 이스케이프
- [ ] `DiscordEmbedFormatterTest` — 색상 십진수 매핑
- [ ] `NotificationSummaryServiceTest` — `shouldSummarize()` CLAUDE/CODEX 포함 시 true, GITHUB 시 false
- [ ] `NotificationSummaryServiceTest` — LLM 실패 시 null 반환 (예외 비전파)
- [ ] `PromptBuilderTest` — `buildNotificationSummaryPrompt` RAG context 포함/미포함 프롬프트 구조
- [ ] `PromptBuilderTest` — `buildDigestSummaryPrompt` 복수 알림 목록 프롬프트 구조

### 통합 테스트 (Testcontainers)
- [ ] `ChannelDeliverySchedulerTest` — RETRY 백오프 3회 후 DEAD 전환
- [ ] `NotificationDigestSchedulerTest` — DIGEST_PENDING → 만료 → LLM 묶음 요약 → SUCCESS
- [ ] `DeliveryFeedControllerTest` — `@WebMvcTest` 페이지네이션, channelType 필터
- [ ] `V14MigrationTest` — Flyway V14 마이그레이션 후 스키마 검증
- [ ] `NotificationSummaryServiceIntegrationTest` — CLAUDE 소스 알림 → `ai_summary` DB 저장 확인
- [ ] `NotificationSummaryServiceIntegrationTest` — GITHUB 소스 알림 → 요약 skip 확인
- [ ] `OllamaLlmProviderTest` — LLM 타임아웃 시 `AiException` 발생 확인
