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

- [ ] `AiUsageLog.java` 생성
  - [ ] 필드: `id`, `userId`, `notificationId`, `model`, `inputTokens`, `outputTokens`, `totalTokens`(삽입·수정 불가), `sessionAt`, `createdAt`, `updatedAt`, `deletedAt`
  - [ ] `@Builder`, `@NoArgsConstructor(AccessLevel.PROTECTED)`, `@Getter` 적용
  - [ ] `softDelete()` 메서드 구현

### Projection Interfaces

- [ ] `AiUsageDataPoint.java` 인터페이스 생성
  - [ ] `getPeriodLabel()`, `getTotalInput()`, `getTotalOutput()`, `getSessionCount()` 메서드 정의
- [ ] `ModelUsageDataPoint.java` 인터페이스 생성
  - [ ] `getModel()`, `getTotalTokens()`, `getSessionCount()` 메서드 정의

### AiUsageLogRepository

- [ ] `AiUsageLogRepository.java` 생성
  - [ ] `findDailyInRange(Long userId, Instant since, Instant until)` — native SQL, `to_char(..., 'YYYY-MM-DD')`
  - [ ] `findWeeklyInRange(Long userId, Instant since, Instant until)` — native SQL, `to_char(..., 'IYYY-"W"IW')`
  - [ ] `findMonthlyInRange(Long userId, Instant since, Instant until)` — native SQL, `to_char(..., 'YYYY-MM')`
  - [ ] `findModelDistributionInRange(Long userId, Instant since, Instant until)` — native SQL
  - [ ] `sumInputTokensInRange(Long userId, Instant since, Instant until)` — JPQL
  - [ ] `sumOutputTokensInRange(Long userId, Instant since, Instant until)` — JPQL
  - [ ] `countSessionsInRange(Long userId, Instant since, Instant until)` — JPQL
  - [ ] `existsByNotificationId(Long notificationId)` — 파생 메서드

### AiUsageGranularity Enum

- [ ] `AiUsageGranularity.java` 생성 (`com.notio.analytics.dto`)
  - [ ] `DAILY`, `WEEKLY`, `MONTHLY` 값 정의
  - [ ] `from(String value)` — 파싱 실패 시 `NotioException(INVALID_REQUEST)` throw
  - [ ] `maxDays()` — DAILY: 90, WEEKLY: 365, MONTHLY: 730
  - [ ] `defaultDays()` — DAILY: 7, WEEKLY: 56, MONTHLY: 365

### AiUsageResponse Record DTO

- [ ] `AiUsageResponse.java` 생성 (`com.notio.analytics.dto`)
  - [ ] 최상위 필드: `granularity`, `startDate`, `endDate`, `totalInputTokens`, `totalOutputTokens`, `totalSessions`, `mostUsedModel`(nullable), `trend`, `modelDistribution`
  - [ ] 중첩 record `AiUsagePeriodPoint(String label, long inputTokens, long outputTokens, long sessions)` 정의
  - [ ] 중첩 record `AiUsageModelShare(String model, long totalTokens, long sessions)` 정의

### AiUsageLogService

- [ ] `AiUsageLogService.java` 생성 (`com.notio.analytics.service`)
  - [ ] `logFromNotification(Notification saved)` 구현
    - [ ] `source != CLAUDE` → 즉시 return
    - [ ] `existsByNotificationId` 중복 체크 → 중복이면 return
    - [ ] `metadata` JSON 파싱 → `usage.input_tokens`, `usage.output_tokens`, `model` 추출
    - [ ] 입출력 토큰 모두 0 → return (fallback 페이로드 케이스)
    - [ ] `session_at`: `notification.timestamp` 파싱, 실패 시 `notification.created_at` fallback
    - [ ] `AiUsageLog` 빌드 후 save
    - [ ] `event=ai_usage_log_created` / `event=ai_usage_log_skip` 로그 기록
  - [ ] `getAiUsage(Long userId, AiUsageGranularity granularity, LocalDate startDate, LocalDate endDate)` 구현
    - [ ] `startDate`/`endDate` null → `granularity.defaultDays()`로 자동 설정
    - [ ] `endDate - startDate > granularity.maxDays()` → `NotioException(INVALID_REQUEST)`
    - [ ] `startDate > endDate` → `NotioException(INVALID_REQUEST)`
    - [ ] `since`/`until` UTC Instant 변환 (`endDate.plusDays(1)` 사용)
    - [ ] 총합·세션 수 조회 (InRange 쿼리)
    - [ ] 트렌드 조회 (granularity별 native 쿼리)
    - [ ] 모델 분포 조회
    - [ ] `AiUsageResponse` 조립 후 반환

---

## Phase 3: 기존 파일 수정

### NotificationService

- [ ] `AiUsageLogService` 생성자 주입 추가
- [ ] `saveNotification()` 내부 Branch C 아래 **Branch D** 추가
  - [ ] `CompletableFuture.runAsync(() -> aiUsageLogService.logFromNotification(saved), VIRTUAL_THREAD_EXECUTOR)` 비동기 호출
  - [ ] `catch (Exception e)` → `log.warn("event=ai_usage_log_failed notification_id={} user_id={} exception_type={}", ...)` 로그

### AnalyticsController

- [ ] `AiUsageLogService` 생성자 주입 추가
- [ ] `GET /api/v1/analytics/ai-usage` 엔드포인트 추가
  - [ ] `@RequestParam(defaultValue = "DAILY") String granularity`
  - [ ] `@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate`
  - [ ] `@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate`
  - [ ] `Authentication` 파라미터로 현재 userId 추출
  - [ ] `ApiResponse.success(aiUsageLogService.getAiUsage(...))` 반환

---

## Phase 4: 테스트

- [ ] `AiUsageLogServiceTest` 단위 테스트 작성
  - [ ] `logFromNotification` — 정상 저장 케이스
  - [ ] `logFromNotification` — source != CLAUDE → skip
  - [ ] `logFromNotification` — 중복 notification_id → skip
  - [ ] `logFromNotification` — 토큰 모두 0 → skip
  - [ ] `logFromNotification` — metadata 파싱 실패 → skip (경고 로그)
  - [ ] `getAiUsage` — 날짜 범위 초과 → `INVALID_REQUEST`
  - [ ] `getAiUsage` — startDate > endDate → `INVALID_REQUEST`
  - [ ] `getAiUsage` — 기본값 적용 (null 파라미터)
- [ ] `@WebMvcTest(AnalyticsController)` 슬라이스 테스트
  - [ ] 파라미터 없음 → 200 + DAILY 기본값 응답
  - [ ] `granularity=INVALID` → 400 + INVALID_REQUEST
  - [ ] `granularity=DAILY&startDate=2026-01-01&endDate=2026-12-31` (91일 초과) → 400

---

## 최종 검증

- [ ] Flyway V15 적용 확인: `SELECT * FROM flyway_schema_history WHERE version = '15'`
- [ ] 테스트 webhook POST → `ai_usage_logs` 행 생성 + `event=ai_usage_log_created` 로그 확인
- [ ] 토큰 0 페이로드 → 행 미생성 + `event=ai_usage_log_skip reason=zero_tokens` 확인
- [ ] 동일 notification_id 중복 전송 → 1행만 존재 확인
- [ ] `GET /api/v1/analytics/ai-usage` (파라미터 없음) → 200 + 기본 범위 응답
- [ ] `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-04-01&endDate=2026-04-30` → 30개 이하 포인트, label YYYY-MM-DD 포맷
- [ ] `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-01-01&endDate=2026-12-31` → 400 + INVALID_REQUEST
- [ ] `startDate > endDate` → 400 + INVALID_REQUEST
- [ ] `granularity=INVALID` → 400 + INVALID_REQUEST
