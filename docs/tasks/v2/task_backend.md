# Task: Backend 개발 체크리스트 (라우팅 모드별 동작 개선)

> **대상 버전**: v2.1 (fix)
> **작성일**: 2026-05-14
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## Phase 1: 파이프라인 분리 — 라우팅과 요약 독립 실행

**파일**: `backend/src/main/java/com/notio/notification/service/NotificationService.java`

- [x] Branch B의 기존 `try { summarize(saved); channelRouter.route(saved); }` 블록 제거
- [x] Branch B: `channelRouter.route(saved)`를 독립 비동기 작업(`CompletableFuture.runAsync`)으로 분리
  - [x] 실패 시 `event=channel_routing_failed notification_id={} user_id={}` 로그 출력
- [x] Branch C: `notificationSummaryService.summarize(saved)`를 독립 비동기 작업으로 분리
  - [x] 실패 시 `event=notification_summarize_failed notification_id={} user_id={}` 로그 출력
- [x] 두 작업 모두 `VIRTUAL_THREAD_EXECUTOR` 사용 확인

---

## Phase 2: IMMEDIATE — `buildMessage()` 데드 코드 제거

**파일**: `backend/src/main/java/com/notio/channel/ChannelRouter.java`

- [x] `buildMessage()` 내 `aiSummary != null` 분기 제거
- [x] `notification.getBody()`를 직접 사용하도록 변경
- [x] 반환 `ChannelMessage` 생성자에 필요한 필드(`id`, `title`, `body`, `source`, `priority`, `externalUrl`, `createdAt`) 확인

---

## Phase 3: DIGEST — 기간 내 알림 없으면 전달 스킵

**파일**: `backend/src/main/java/com/notio/channel/NotificationDigestScheduler.java`

- [x] `processDigestForChannel()` 상단에 `pendingLogs.isEmpty()` early return 추가
  - [x] 스킵 시 `event=digest_skipped_no_notifications channel_id={}` debug 로그 출력
- [x] 알림 DB 조회 후 `notifications`가 비어 있으면 `DEAD` 처리하는 기존 분기 동작 확인

---

## Phase 4: DIGEST — 전달 실패 시 `nextRetryAt` 설정

**파일**: `backend/src/main/java/com/notio/channel/NotificationDigestScheduler.java`

- [x] 실패 처리 블록에서 `retryAt = Instant.now().plus(5, ChronoUnit.MINUTES)` 계산
- [x] `result.retryable()` 분기
  - [x] `true`: `status = RETRY`, `nextRetryAt = retryAt` 설정
  - [x] `false`: `status = DEAD` 설정
- [x] 두 경우 모두 `lastError = result.errorMessage()` 설정
- [x] `ChronoUnit` import 추가 확인

---

## Phase 5: DIGEST — 묶음 메시지 헤더 개선

**파일**: `backend/src/main/java/com/notio/channel/NotificationDigestScheduler.java`

- [x] `sourceSummary` 계산: 알림 목록의 source를 `distinct → sorted → joining(", ")` 처리
- [x] `maxPriority` 계산: `notifications` 스트림에서 `max(Comparator.naturalOrder())` 사용, 기본값 `MEDIUM`
- [x] `ChannelMessage` 생성 시 제목을 `"[묶음 알림] " + size + "개 · " + sourceSummary` 형식으로 변경
- [x] `priority` 필드를 하드코딩 `MEDIUM` 대신 `maxPriority`로 교체

---

## Phase 6: 검증

- [ ] **IMMEDIATE 지연 개선**: 알림 수신 후 채널 전달이 LLM 요약 대기 없이 즉시 이루어지는지 확인
- [ ] **IMMEDIATE 원본 body**: 전달된 메시지 body가 원본 body 그대로인지 확인 (aiSummary 아님)
- [ ] **DIGEST 빈 알림 스킵**: 설정 기간 내 알림이 없으면 채널에 아무것도 전달되지 않는지 확인
- [ ] **DIGEST 정상 동작**: 여러 알림 수신 후 기간 만료 시 LLM 묶음 요약 메시지 수신 확인
- [ ] **DIGEST 헤더**: 복수 소스(GITHUB + SLACK 등) 알림 혼재 시 제목에 소스 목록 노출 확인
- [ ] **DIGEST 재시도**: 채널 전달 실패(retryable) 시 5분 후 `ChannelDeliveryScheduler`가 재처리하는지 확인
- [ ] **로그 독립성**: `event=channel_routing_*`와 `event=notification_summarize_*`가 순서에 무관하게 독립적으로 출력되는지 확인
