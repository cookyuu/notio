# Backend MVP 체크리스트

> Spring Boot 4.x · Java 25 · Gradle · MVP (Phase 0)

---

## 환경 세팅

- [ ] Spring Boot 4.x 프로젝트 생성 (Gradle Kotlin DSL)
- [ ] Java 25 설정 (`toolchain { languageVersion = JavaLanguageVersion.of(25) }`)
- [ ] 패키지 구조 생성 (`com.notio.*`)
- [ ] `application.yml` / `application-local.yml` 분리
- [ ] Flyway 마이그레이션 설정
- [ ] Docker Compose에서 PostgreSQL + Redis + Ollama 연결 확인
- [ ] SpringDoc OpenAPI (Swagger UI) 설정
- [ ] `ApiResponse<T>` 공통 응답 래퍼 구현
- [ ] `GlobalExceptionHandler` + `ErrorCode` 구현

---

## Webhook 수신

- [ ] `WebhookHandler` 인터페이스 정의
- [ ] `WebhookVerifier` 인터페이스 정의 (전략 패턴)
- [ ] `WebhookDispatcher` 구현 (Handler 자동 탐색)
- [ ] `ClaudeHookHandler` — Bearer 토큰 검증
- [ ] `SlackWebhookHandler` — HMAC-SHA256 검증
- [ ] `GithubWebhookHandler` — HMAC-SHA256 검증
- [ ] `NotificationEvent` DTO 정의
- [ ] `WebhookController` — `POST /api/v1/webhook/{source}`
- [ ] 잘못된 서명 → 401 응답 확인

---

## 알림 도메인

- [ ] `Notification` 엔티티 정의
- [ ] `NotificationSource` enum (CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL)
- [ ] `NotificationPriority` enum (HIGH, MEDIUM, LOW)
- [ ] `V1__create_notifications.sql` 마이그레이션
- [ ] `NotificationRepository` + QueryDSL 필터 구현
- [ ] `NotificationService` 구현
  - [ ] `saveFromEvent(NotificationEvent)` — 저장
  - [ ] `findAll(filter, pageable)` — 목록 조회
  - [ ] `markRead(id)` — 읽음 처리
  - [ ] `markAllRead()` — 전체 읽음
  - [ ] `delete(id)` — 소프트 삭제
  - [ ] `countUnread()` — 미읽음 수 (Redis 캐시)
- [ ] `NotificationController` REST API 구현
  - [ ] `GET  /api/v1/notifications` — 목록 (필터, 페이지네이션)
  - [ ] `GET  /api/v1/notifications/{id}` — 상세 + 읽음 처리
  - [ ] `POST /api/v1/notifications/read-all` — 전체 읽음
  - [ ] `DELETE /api/v1/notifications/{id}` — 삭제

---

## 푸시 발송

- [ ] `Device` 엔티티 + `V3__create_devices.sql`
- [ ] Firebase Admin SDK 의존성 추가 + 설정
- [ ] `PushService.sendPush(notificationId)` 구현
- [ ] `POST /api/v1/devices/register` — FCM 토큰 등록
- [ ] 알림 저장 후 FCM 푸시 발송 연동 (동기)
- [ ] Android 실기기 푸시 수신 확인

---

## AI 채팅 (Spring AI + Ollama)

- [ ] Spring AI Ollama 의존성 + `SpringAiConfig` 설정
- [ ] `ChatService.chat(ChatRequest)` — RAG 포함 채팅
- [ ] `ChatService.streamChat(ChatRequest)` — SSE 스트리밍
- [ ] `DailySummaryService.getSummary()` — Redis 캐시 24h
- [ ] `ChatController`
  - [ ] `POST /api/v1/chat` — 단건 응답
  - [ ] `GET  /api/v1/chat/stream` — SSE 스트리밍
  - [ ] `GET  /api/v1/chat/daily-summary` — 오늘 요약
  - [ ] `GET  /api/v1/chat/history` — 채팅 이력

---

## 할일

- [ ] `Todo` 엔티티 + `V2__create_todos.sql`
- [ ] `TodoStatus` enum (PENDING, IN_PROGRESS, DONE)
- [ ] `TodoService.createFromNotification(request)` — LLM 제목 자동 생성
- [ ] `TodoController`
  - [ ] `POST  /api/v1/todos` — 생성
  - [ ] `GET   /api/v1/todos` — 목록
  - [ ] `PATCH /api/v1/todos/{id}` — 상태 변경

---

## 분석

- [ ] `AnalyticsService.getWeeklySummary()` 구현
- [ ] `GET /api/v1/analytics/weekly` — 주간 통계

---

## 테스트

- [ ] `NotificationServiceTest` 단위 테스트
- [ ] `WebhookDispatcherTest` 단위 테스트
- [ ] `NotificationControllerTest` 슬라이스 테스트
- [ ] `WebhookControllerTest` — 서명 검증 시나리오
- [ ] Testcontainers PostgreSQL + Redis 통합 테스트 환경 구성

---

## 코드 품질

- [ ] Checkstyle 설정 (`google_checks.xml` 기반)
- [ ] SpotBugs 설정
- [ ] `.editorconfig` 설정
- [ ] GitHub Actions `ci-backend.yml` — `backend/**` 변경 시만 실행
