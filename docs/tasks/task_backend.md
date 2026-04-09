# Backend 로드맵 체크리스트

> Spring Boot 4.x · Java 25 · Gradle · Backend Phase Checklist

---

## Phase 0. MVP 모놀리스 구축

> 목표: 단일 Spring Boot 서비스에서 Webhook, Notification, Chat, Todo, Push, Analytics MVP 완성

### 환경 세팅

- [x] Spring Boot 4.x 프로젝트 생성 (Gradle Kotlin DSL)
- [x] Java 25 설정 (`toolchain { languageVersion = JavaLanguageVersion.of(25) }`)
- [x] 패키지 구조 생성 (`com.notio.*`)
- [x] `application.yml` / `application-local.yml` 분리
- [x] Flyway 마이그레이션 설정
- [x] Docker Compose에서 PostgreSQL + Redis 연결 확인 (`Ollama`는 현재 미사용)
- [x] SpringDoc OpenAPI (Swagger UI) 설정
- [x] `ApiResponse<T>` 공통 응답 래퍼 구현
- [x] `GlobalExceptionHandler` + `ErrorCode` 구현

### Webhook 수신

- [x] `WebhookHandler` 인터페이스 정의
- [x] `WebhookVerifier` 인터페이스 정의 (전략 패턴)
- [x] `WebhookDispatcher` 구현 (Handler 자동 탐색)
- [x] `ClaudeHookHandler` 구현 및 Bearer 토큰 검증
- [x] `SlackWebhookHandler` 구현 및 HMAC-SHA256 검증
- [x] `GithubWebhookHandler` 구현 및 HMAC-SHA256 검증
- [x] `NotificationEvent` DTO 정의
- [x] `WebhookController` 구현 (`POST /api/v1/webhook/{source}`)
- [ ] 잘못된 서명 요청에 대해 401 응답 확인

### 알림 도메인

- [ ] `Notification` 엔티티 정의
- [x] `NotificationSource` enum 정의 (`CLAUDE`, `SLACK`, `GITHUB`, `GMAIL`, `INTERNAL`)
- [x] `NotificationPriority` enum 정의 (`HIGH`, `MEDIUM`, `LOW`)
- [ ] `V1__create_notifications.sql` 작성
- [ ] `NotificationRepository` 구현
- [ ] QueryDSL 기반 알림 필터 구현
- [ ] `NotificationService` 구현
- [ ] `saveFromEvent(NotificationEvent)` 구현
- [ ] `findAll(filter, pageable)` 구현
- [ ] `markRead(id)` 구현
- [ ] `markAllRead()` 구현
- [ ] `delete(id)` 구현 (소프트 삭제)
- [ ] `countUnread()` 구현 (Redis 캐시)
- [ ] `NotificationController` REST API 구현
- [ ] `GET /api/v1/notifications` 구현 (필터, 페이지네이션)
- [ ] `GET /api/v1/notifications/{id}` 구현 (상세 + 읽음 처리)
- [ ] `POST /api/v1/notifications/read-all` 구현
- [ ] `DELETE /api/v1/notifications/{id}` 구현

### 푸시 발송

- [ ] `Device` 엔티티 정의
- [ ] `V3__create_devices.sql` 작성
- [ ] Firebase Admin SDK 의존성 추가 및 설정
- [ ] `PushService.sendPush(notificationId)` 구현
- [ ] `POST /api/v1/devices/register` 구현 (FCM 토큰 등록)
- [ ] 알림 저장 후 FCM 푸시 발송 연동 (동기)
- [ ] Android 실기기 푸시 수신 확인

### AI 채팅

- [ ] Spring AI Ollama 의존성 추가
- [ ] `SpringAiConfig` 설정
- [ ] `ChatService.chat(ChatRequest)` 구현
- [ ] `ChatService.streamChat(ChatRequest)` 구현 (SSE 스트리밍)
- [ ] `DailySummaryService.getSummary()` 구현 (Redis 캐시 24h)
- [ ] `ChatController` 구현
- [ ] `POST /api/v1/chat` 구현
- [ ] `GET /api/v1/chat/stream` 구현
- [ ] `GET /api/v1/chat/daily-summary` 구현
- [ ] `GET /api/v1/chat/history` 구현

### 할일

- [ ] `Todo` 엔티티 정의
- [ ] `V2__create_todos.sql` 작성
- [ ] `TodoStatus` enum 정의 (`PENDING`, `IN_PROGRESS`, `DONE`)
- [ ] `TodoService.createFromNotification(request)` 구현 (LLM 제목 자동 생성)
- [ ] `TodoController` 구현
- [ ] `POST /api/v1/todos` 구현
- [ ] `GET /api/v1/todos` 구현
- [ ] `PATCH /api/v1/todos/{id}` 구현

### 분석

- [ ] `AnalyticsService.getWeeklySummary()` 구현
- [ ] `GET /api/v1/analytics/weekly` 구현

### 테스트 및 품질

- [ ] `NotificationServiceTest` 단위 테스트
- [ ] `WebhookDispatcherTest` 단위 테스트
- [ ] `NotificationControllerTest` 슬라이스 테스트
- [ ] `WebhookControllerTest` 작성 (서명 검증 시나리오)
- [ ] Testcontainers PostgreSQL + Redis 통합 테스트 환경 구성
- [ ] Checkstyle 설정 (`google_checks.xml` 기반)
- [ ] SpotBugs 설정
- [ ] `.editorconfig` 설정
- [ ] GitHub Actions `ci-backend.yml` 구성 (`backend/**` 변경 시만 실행)

---

## Phase 1. AI Service 분리

> 목표: LLM, RAG, 요약/생성 기능을 Python FastAPI 기반 AI 서비스로 분리

### 서비스 분리 준비

- [ ] AI 관련 Spring 서비스 경계를 명확히 분리 (`ChatService`, `DailySummaryService`, `TodoService`의 AI 책임 식별)
- [ ] `LlmProvider` 인터페이스 또는 외부 AI 호출 추상화 계층 정의
- [ ] 모놀리스에서 AI 호출용 클라이언트 계층 설계
- [ ] AI 장애 시 fallback 정책 정의

### Python AI 서비스 구축

- [ ] FastAPI 프로젝트 초기화
- [ ] Poetry 기반 의존성 관리 구성
- [ ] LangChain 연동
- [ ] Ollama 연동
- [ ] Celery + Redis 비동기 작업 기반 준비
- [ ] 프롬프트 중앙화 구조 (`PromptBuilder` 또는 동등한 모듈) 설계

### 기능 이전

- [ ] 채팅 응답 생성 로직을 AI 서비스로 이전
- [ ] SSE 또는 스트리밍 응답 프로토콜 정의
- [ ] 일일 요약 생성 로직을 AI 서비스로 이전
- [ ] 할일 제목/요약 생성 로직을 AI 서비스로 이전
- [ ] RAG 검색 및 임베딩 파이프라인 이전

### 운영 및 검증

- [ ] 모놀리스 ↔ AI 서비스 통신 설정
- [ ] 요청/응답 timeout, retry 정책 적용
- [ ] AI 서비스 헬스체크 및 모니터링 지표 추가
- [ ] Phase 0 API 호환성 유지 여부 검증

---

## Phase 2. Notification / Webhook 서비스 분리

> 목표: 알림 저장/조회와 Webhook 수신을 독립 서비스로 추출하고 이벤트 기반 흐름 도입

### 서비스 분리 설계

- [ ] Notification 도메인 API와 Webhook 수신 API의 경계 분리
- [ ] 서비스별 DB 소유 전략 정리
- [ ] 공유 모델 제거 및 서비스 간 계약 DTO 정의
- [ ] 분리 대상 API 라우팅 전략 수립

### Webhook Service

- [ ] Webhook 전용 Spring Boot 서비스 생성
- [ ] Claude / Slack / GitHub 검증 로직 이전
- [ ] 소스별 `WebhookHandler` 확장 구조 유지
- [ ] Webhook 수신 후 내부 이벤트 발행 구조 적용

### Notification Service

- [ ] Notification 전용 Spring Boot 서비스 생성
- [ ] 알림 저장/조회/읽음/삭제 기능 이전
- [ ] 미읽음 수 캐시 전략 이전
- [ ] 알림 필터/페이지네이션 API 이전

### 이벤트 기반 통합

- [ ] Kafka 도입
- [ ] Webhook Service → Notification Service 이벤트 계약 정의
- [ ] 알림 생성 이벤트 스키마 정의
- [ ] 재처리 / 중복 처리 / 멱등성 정책 적용

### 운영 및 배포

- [ ] Notification / Webhook 서비스 독립 배포 파이프라인 구성
- [ ] 서비스별 로그/메트릭 분리
- [ ] 장애 시 이벤트 유실 방지 전략 검증

---

## Phase 3. Chat / Todo 서비스 분리

> 목표: 채팅과 할일 도메인을 독립 배포 및 독립 스케일 가능한 서비스로 추출

### Chat Service 분리

- [ ] Chat 전용 Spring Boot 서비스 생성
- [ ] 채팅 이력 저장/조회 책임 이전
- [ ] 스트리밍 채팅 API 이전
- [ ] AI 서비스 연동 책임을 Chat Service로 이동
- [ ] 대화 컨텍스트 관리 전략 정리

### Todo Service 분리

- [ ] Todo 전용 Spring Boot 서비스 생성
- [ ] 할일 생성/조회/상태 변경 책임 이전
- [ ] 알림 기반 할일 생성 이벤트 연동
- [ ] Todo 상태 변경 이벤트 정의

### 서비스 간 연동

- [ ] Notification ↔ Todo 연계 계약 정리
- [ ] Chat ↔ AI Service API 계약 고정
- [ ] 필요한 REST / Kafka 경로 분리
- [ ] 서비스 간 인증 또는 내부 토큰 정책 정의

### 운영 및 검증

- [ ] Chat / Todo 서비스 독립 테스트 파이프라인 구성
- [ ] 서비스 분리 후 API 회귀 테스트 수행
- [ ] 데이터 정합성 검증

---

## Phase 4. Analytics / Auth / Gateway 완성

> 목표: 멀티유저 SaaS 확장을 위한 플랫폼 서비스와 API Gateway 체계 완성

### API Gateway

- [ ] API Gateway 서비스 생성
- [ ] 서비스별 라우팅 규칙 구성
- [ ] 공통 인증/인가 필터 이전
- [ ] Rate Limiting 정책 적용
- [ ] Resilience4j 등 공통 복원력 정책 적용

### Auth Service

- [ ] Auth 전용 서비스 생성
- [ ] JWT 발급/검증 책임 이전
- [ ] 사용자/디바이스/세션 모델 설계
- [ ] 멀티유저 대응 인증 플로우 구축

### Analytics Service

- [ ] Analytics 전용 서비스 생성
- [ ] 주간 통계 및 리포트 생성 로직 이전
- [ ] Kafka 기반 이벤트 수집 파이프라인 구축
- [ ] 장기 집계 데이터 모델 설계

### 플랫폼 운영 고도화

- [ ] Prometheus + Grafana 모니터링 구성
- [ ] Loki / Promtail 로그 수집 구성
- [ ] Zipkin 또는 OpenTelemetry 기반 트레이싱 구성
- [ ] Kubernetes 배포 전략 수립
- [ ] 서비스별 HPA / 오토스케일 정책 정의

### 최종 검증

- [ ] 모놀리스 잔여 책임 제거
- [ ] 서비스별 독립 배포 및 롤백 절차 검증
- [ ] 장애 격리 시나리오 점검
- [ ] 멀티서비스 E2E 테스트 완료
