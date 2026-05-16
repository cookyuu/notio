# Task: AI Token Usage Analytics — Backend 구현 체크리스트

> **대상 버전**: v2.3
> **작성일**: 2026-05-15
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## Phase 1: V15 Migration

- [x] `db/migration/V15__create_ai_usage_logs.sql` 파일 생성
  - [x] `ai_usage_logs` 테이블 생성 (id, user_id, notification_id, model, input_tokens, output_tokens, total_tokens GENERATED ALWAYS, session_at, created_at, updated_at, deleted_at)
  - [x] `fk_ai_usage_logs_users` FK 제약조건 추가
  - [x] `fk_ai_usage_logs_notifications` FK 제약조건 추가
  - [x] `idx_ai_usage_logs_user_session_at` 인덱스 생성 (`user_id, session_at DESC WHERE deleted_at IS NULL`)
  - [x] `idx_ai_usage_logs_model` 인덱스 생성 (`user_id, model WHERE deleted_at IS NULL`)
  - [x] `uq_ai_usage_logs_notification_id` UNIQUE 인덱스 생성 (idempotency 보장)

---

## Phase 2: 신규 파일 — `com.notio.analytics.domain`

### AiUsageLog Entity

- [x] `AiUsageLog.java` 생성
  - [x] 필드: `id`, `userId`, `notificationId`, `model`, `inputTokens`, `outputTokens`, `totalTokens`(삽입·수정 불가), `sessionAt`, `createdAt`, `updatedAt`, `deletedAt`
  - [x] `@Builder`, `@NoArgsConstructor(AccessLevel.PROTECTED)`, `@Getter` 적용
  - [x] `softDelete()` 메서드 구현

### Projection Interfaces

- [x] `AiUsageDataPoint.java` 인터페이스 생성
  - [x] `getPeriodLabel()`, `getTotalInput()`, `getTotalOutput()`, `getSessionCount()` 메서드 정의
- [x] `ModelUsageDataPoint.java` 인터페이스 생성
  - [x] `getModel()`, `getTotalTokens()`, `getSessionCount()` 메서드 정의

### AiUsageLogRepository

- [x] `AiUsageLogRepository.java` 생성
  - [x] `findDailyInRange(Long userId, Instant since, Instant until)` — native SQL, `to_char(..., 'YYYY-MM-DD')`
  - [x] `findWeeklyInRange(Long userId, Instant since, Instant until)` — native SQL, `to_char(..., 'IYYY-"W"IW')`
  - [x] `findMonthlyInRange(Long userId, Instant since, Instant until)` — native SQL, `to_char(..., 'YYYY-MM')`
  - [x] `findModelDistributionInRange(Long userId, Instant since, Instant until)` — native SQL
  - [x] `sumInputTokensInRange(Long userId, Instant since, Instant until)` — JPQL
  - [x] `sumOutputTokensInRange(Long userId, Instant since, Instant until)` — JPQL
  - [x] `countSessionsInRange(Long userId, Instant since, Instant until)` — JPQL
  - [x] `existsByNotificationId(Long notificationId)` — 파생 메서드

### AiUsageGranularity Enum

- [x] `AiUsageGranularity.java` 생성 (`com.notio.analytics.dto`)
  - [x] `DAILY`, `WEEKLY`, `MONTHLY` 값 정의
  - [x] `from(String value)` — 파싱 실패 시 `NotioException(INVALID_REQUEST)` throw
  - [x] `maxDays()` — DAILY: 90, WEEKLY: 365, MONTHLY: 730
  - [x] `defaultDays()` — DAILY: 7, WEEKLY: 56, MONTHLY: 365

### AiUsageResponse Record DTO

- [x] `AiUsageResponse.java` 생성 (`com.notio.analytics.dto`)
  - [x] 최상위 필드: `granularity`, `startDate`, `endDate`, `totalInputTokens`, `totalOutputTokens`, `totalSessions`, `mostUsedModel`(nullable), `trend`, `modelDistribution`
  - [x] 중첩 record `AiUsagePeriodPoint(String label, long inputTokens, long outputTokens, long sessions)` 정의
  - [x] 중첩 record `AiUsageModelShare(String model, long totalTokens, long sessions)` 정의

### AiUsageLogService

- [x] `AiUsageLogService.java` 생성 (`com.notio.analytics.service`)
  - [x] `logFromNotification(Notification saved)` 구현
    - [x] `source != CLAUDE` → 즉시 return
    - [x] `existsByNotificationId` 중복 체크 → 중복이면 return
    - [x] `metadata` JSON 파싱 → `usage.input_tokens`, `usage.output_tokens`, `model` 추출
    - [x] 입출력 토큰 모두 0 → return (fallback 페이로드 케이스)
    - [x] `session_at`: `notification.timestamp` 파싱, 실패 시 `notification.created_at` fallback
    - [x] `AiUsageLog` 빌드 후 save
    - [x] `event=ai_usage_log_created` / `event=ai_usage_log_skip` 로그 기록
  - [x] `getAiUsage(Long userId, AiUsageGranularity granularity, LocalDate startDate, LocalDate endDate)` 구현
    - [x] `startDate`/`endDate` null → `granularity.defaultDays()`로 자동 설정
    - [x] `endDate - startDate > granularity.maxDays()` → `NotioException(INVALID_REQUEST)`
    - [x] `startDate > endDate` → `NotioException(INVALID_REQUEST)`
    - [x] `since`/`until` UTC Instant 변환 (`endDate.plusDays(1)` 사용)
    - [x] 총합·세션 수 조회 (InRange 쿼리)
    - [x] 트렌드 조회 (granularity별 native 쿼리)
    - [x] 모델 분포 조회
    - [x] `AiUsageResponse` 조립 후 반환

---

## Phase 3: 기존 파일 수정

### NotificationService

- [x] `AiUsageLogService` 생성자 주입 추가
- [x] `saveNotification()` 내부 Branch C 아래 **Branch D** 추가
  - [x] `CompletableFuture.runAsync(() -> aiUsageLogService.logFromNotification(saved), VIRTUAL_THREAD_EXECUTOR)` 비동기 호출
  - [x] `catch (Exception e)` → `log.warn("event=ai_usage_log_failed notification_id={} user_id={} exception_type={}", ...)` 로그

### AnalyticsController

- [x] `AiUsageLogService` 생성자 주입 추가
- [x] `GET /api/v1/analytics/ai-usage` 엔드포인트 추가
  - [x] `@RequestParam(defaultValue = "DAILY") String granularity`
  - [x] `@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate`
  - [x] `@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate`
  - [x] `Authentication` 파라미터로 현재 userId 추출
  - [x] `ApiResponse.success(aiUsageLogService.getAiUsage(...))` 반환

---

## Phase 4: 테스트

- [x] `AiUsageLogServiceTest` 단위 테스트 작성
  - [x] `logFromNotification` — 정상 저장 케이스
  - [x] `logFromNotification` — source != CLAUDE → skip
  - [x] `logFromNotification` — 중복 notification_id → skip
  - [x] `logFromNotification` — 토큰 모두 0 → skip
  - [x] `logFromNotification` — metadata 파싱 실패 → skip (경고 로그)
  - [x] `getAiUsage` — 날짜 범위 초과 → `INVALID_REQUEST`
  - [x] `getAiUsage` — startDate > endDate → `INVALID_REQUEST`
  - [x] `getAiUsage` — 기본값 적용 (null 파라미터)
- [x] `@WebMvcTest(AnalyticsController)` 슬라이스 테스트
  - [x] 파라미터 없음 → 200 + DAILY 기본값 응답
  - [x] `granularity=INVALID` → 400 + INVALID_REQUEST
  - [x] `granularity=DAILY&startDate=2026-01-01&endDate=2026-12-31` (91일 초과) → 400

---

## 최종 검증

- [ ] Flyway V15 적용 확인: `SELECT * FROM flyway_schema_history WHERE version = '15'`
- [ ] 테스트 webhook POST → `ai_usage_logs` 행 생성 + `event=ai_usage_log_created` 로그 확인
- [x] 토큰 0 페이로드 → 행 미생성 + `event=ai_usage_log_skip reason=zero_tokens` 확인 — `AiUsageLogServiceTest#logFromNotificationSkipsWhenBothTokensAreZero`
- [x] 동일 notification_id 중복 전송 → 1행만 존재 확인 — `AiUsageLogServiceTest#logFromNotificationSkipsWhenNotificationIdAlreadyExists`
- [x] `GET /api/v1/analytics/ai-usage` (파라미터 없음) → 200 + 기본 범위 응답 — `AnalyticsControllerTest#aiUsageReturnsOkWithDailyDefaultWhenNoParams`
- [ ] `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-04-01&endDate=2026-04-30` → 30개 이하 포인트, label YYYY-MM-DD 포맷
- [x] `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-01-01&endDate=2026-12-31` → 400 + INVALID_REQUEST — `AnalyticsControllerTest#aiUsageReturnsBadRequestWhenDateRangeExceedsMaxDays`
- [x] `startDate > endDate` → 400 + INVALID_REQUEST — `AiUsageLogServiceTest#getAiUsageThrowsInvalidRequestWhenStartDateIsAfterEndDate`
- [x] `granularity=INVALID` → 400 + INVALID_REQUEST — `AnalyticsControllerTest#aiUsageReturnsBadRequestWhenGranularityIsInvalid`
