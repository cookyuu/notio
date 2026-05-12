# Notio — Backend 개발 계획서 (plan_backend.md)

> Spring Boot 4.x · Java 25 · Gradle · MVP (Phase 0)

---

## 1. 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 25 |
| 프레임워크 | Spring Boot | 4.0.0 |
| 빌드 | Gradle | 9.0 (Kotlin DSL) |
| ORM | Spring Data JPA (Hibernate) | 7.1.8 |
| QueryDSL | (Phase 1) — 호환 버전 대기 중 | — |
| AI | (Phase 1) Spring AI (Ollama) | 1.0.0-M6 |
| 캐시/큐 | Spring Data Redis | 3.x |
| 메시지 | (Phase 2) Kafka — MVP는 동기 처리 | — |
| DB | PostgreSQL 16 | ankane/pgvector:v0.5.1 |
| 마이그레이션 | Flyway | 11.1.0 |
| 보안 | Spring Security + JWT (jjwt) | 0.12.6 |
| 푸시 | Firebase Admin SDK | 9.4.2 |
| 테스트 | JUnit 5 · Mockito · Testcontainers | 1.20.4 |
| 문서 | Swagger (Springfox) | 3.0.0 |

---

## 2. 폴더 구조

```
backend/
├── build.gradle.kts
├── Dockerfile
├── .env.example
└── src/
    └── main/
        ├── java/com/notio/
        │   ├── NotioApplication.java
        │   │
        │   ├── common/                        # 공통
        │   │   ├── config/
        │   │   │   ├── SecurityConfig.java
        │   │   │   ├── RedisConfig.java
        │   │   │   ├── JpaConfig.java
        │   │   │   └── SpringAiConfig.java
        │   │   ├── exception/
        │   │   │   ├── GlobalExceptionHandler.java
        │   │   │   ├── NotioException.java
        │   │   │   └── ErrorCode.java         # enum
        │   │   ├── response/
        │   │   │   ├── ApiResponse.java        # 공통 응답 래퍼
        │   │   │   └── PageResponse.java
        │   │   └── util/
        │   │       ├── HmacUtils.java
        │   │       └── JwtUtils.java
        │   │
        │   ├── notification/                  # 알림 도메인
        │   │   ├── domain/
        │   │   │   ├── Notification.java      # @Entity
        │   │   │   ├── NotificationSource.java # enum
        │   │   │   └── NotificationPriority.java # enum
        │   │   ├── repository/
        │   │   │   ├── NotificationRepository.java
        │   │   │   └── NotificationRepositoryImpl.java # QueryDSL
        │   │   ├── service/
        │   │   │   └── NotificationService.java
        │   │   ├── dto/
        │   │   │   ├── NotificationResponse.java
        │   │   │   └── NotificationFilterRequest.java
        │   │   └── controller/
        │   │       └── NotificationController.java
        │   │
        │   ├── webhook/                       # Webhook 수신
        │   │   ├── controller/
        │   │   │   └── WebhookController.java
        │   │   ├── handler/
        │   │   │   ├── WebhookHandler.java    # interface
        │   │   │   ├── ClaudeHookHandler.java
        │   │   │   ├── SlackWebhookHandler.java
        │   │   │   └── GithubWebhookHandler.java
        │   │   ├── verifier/
        │   │   │   ├── WebhookVerifier.java   # interface
        │   │   │   ├── HmacWebhookVerifier.java
        │   │   │   └── BearerWebhookVerifier.java
        │   │   ├── dispatcher/
        │   │   │   └── WebhookDispatcher.java
        │   │   └── dto/
        │   │       └── NotificationEvent.java # 내부 이벤트 객체
        │   │
        │   ├── chat/                          # AI 채팅
        │   │   ├── service/
        │   │   │   ├── ChatService.java
        │   │   │   └── DailySummaryService.java
        │   │   ├── dto/
        │   │   │   ├── ChatRequest.java
        │   │   │   └── ChatResponse.java
        │   │   └── controller/
        │   │       └── ChatController.java
        │   │
        │   ├── todo/                          # 할일
        │   │   ├── domain/
        │   │   │   ├── Todo.java
        │   │   │   └── TodoStatus.java        # enum
        │   │   ├── repository/
        │   │   │   └── TodoRepository.java
        │   │   ├── service/
        │   │   │   └── TodoService.java
        │   │   ├── dto/
        │   │   │   ├── CreateTodoRequest.java
        │   │   │   └── TodoResponse.java
        │   │   └── controller/
        │   │       └── TodoController.java
        │   │
        │   ├── push/                          # 푸시 발송
        │   │   ├── domain/
        │   │   │   └── Device.java
        │   │   ├── repository/
        │   │   │   └── DeviceRepository.java
        │   │   ├── service/
        │   │   │   └── PushService.java
        │   │   ├── dto/
        │   │   │   └── RegisterDeviceRequest.java
        │   │   └── controller/
        │   │       └── PushController.java
        │   │
        │   └── analytics/                     # 분석
        │       ├── service/
        │       │   └── AnalyticsService.java
        │       ├── dto/
        │       │   └── WeeklyAnalyticsResponse.java
        │       └── controller/
        │           └── AnalyticsController.java
        │
        └── resources/
            ├── application.yml
            ├── application-local.yml
            ├── application-prod.yml
            └── db/migration/
                ├── V1__create_notifications.sql
                ├── V2__create_todos.sql
                ├── V3__create_devices.sql
                └── V4__create_webhook_sources.sql
```

---

## 3. 네이밍 규칙

### 클래스

| 유형 | 규칙 | 예시 |
|------|------|------|
| Entity | 명사 단수형 | `Notification`, `Todo` |
| Repository | `{Domain}Repository` | `NotificationRepository` |
| Service | `{Domain}Service` | `NotificationService` |
| Controller | `{Domain}Controller` | `NotificationController` |
| DTO (응답) | `{Domain}Response` | `NotificationResponse` |
| DTO (요청) | `{Action}{Domain}Request` | `CreateTodoRequest` |
| Interface | 형용사 또는 동사 | `WebhookHandler`, `WebhookVerifier` |
| Enum | PascalCase | `NotificationSource`, `TodoStatus` |
| Enum 값 | UPPER_SNAKE | `CLAUDE`, `IN_PROGRESS` |
| Exception | `{Domain}Exception` | `NotificationNotFoundException` |
| Config | `{기능}Config` | `SecurityConfig`, `RedisConfig` |

### 메서드

| 유형 | 접두사 | 예시 |
|------|--------|------|
| 단건 조회 | `find` | `findById`, `findBySource` |
| 목록 조회 | `findAll` | `findAllByFilter` |
| 저장 / 생성 | `save`, `create` | `save`, `createFromEvent` |
| 수정 | `update` | `updateStatus` |
| 삭제 | `delete` | `deleteById` |
| 검증 | `verify`, `validate` | `verifySignature` |
| 발송 | `send` | `sendPush` |
| 변환 | `to`, `from` | `toResponse`, `fromEntity` |

### 변수 / 필드

- 일반 변수: `camelCase`
- 상수: `UPPER_SNAKE_CASE`
- Boolean: `is`, `has`, `can` 접두사 (`isRead`, `hasEmbedding`)

### API 엔드포인트

- 소문자 케밥 케이스: `/api/v1/notifications`, `/api/v1/chat/daily-summary`
- 컬렉션은 복수형: `/notifications`, `/todos`, `/devices`
- 동사는 HTTP 메서드로 표현 (URL에 동사 사용 금지)
- 예외: 액션 엔드포인트 `/api/v1/notifications/read-all` (PATCH)

### 패키지

- 모두 소문자: `com.notio.notification`, `com.notio.webhook`
- 도메인별 수평 분리 (레이어별 분리 금지)

---

## 4. 공통 응답 형식

```java
// ApiResponse<T>
{
  "success": true,
  "data": { ... },
  "error": null
}

// 에러 시
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOTIFICATION_NOT_FOUND",
    "message": "알림을 찾을 수 없습니다."
  }
}
```

---

## 5. ErrorCode enum 목록

```
NOTIFICATION_NOT_FOUND       (404)
TODO_NOT_FOUND               (404)
DEVICE_NOT_FOUND             (404)
WEBHOOK_VERIFICATION_FAILED  (401)
UNSUPPORTED_SOURCE           (400)
INVALID_REQUEST              (400)
LLM_UNAVAILABLE              (503)
EMBEDDING_FAILED             (500)
INTERNAL_SERVER_ERROR        (500)
```

---

## 6. DB 스키마 핵심 규칙

- 모든 테이블: `id BIGSERIAL PK`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`
- 삭제: `deleted_at TIMESTAMPTZ` soft delete
- 컬럼명: `snake_case`
- 인덱스명: `idx_{테이블}_{컬럼}` (예: `idx_notifications_source`)
- FK명: `fk_{테이블}_{참조테이블}` (예: `fk_todos_notifications`)

---

## 7. application.yml 구조

```yaml
spring:
  datasource:
    url: ${NOTIO_DB_URL}
    username: ${NOTIO_DB_USER}
    password: ${NOTIO_DB_PASSWORD}
  data:
    redis:
      host: ${NOTIO_REDIS_HOST:localhost}
      port: ${NOTIO_REDIS_PORT:6379}
  ai:
    ollama:
      base-url: ${NOTIO_OLLAMA_URL:http://localhost:11434}
      chat:
        model: ${NOTIO_LLM_MODEL:llama3.2:3b}
      embedding:
        model: ${NOTIO_EMBED_MODEL:nomic-embed-text}

notio:
  jwt:
    secret: ${NOTIO_JWT_SECRET}
    expiry-ms: ${NOTIO_JWT_EXPIRY_MS:86400000}
  webhook:
    slack-signing-secret: ${NOTIO_SLACK_SECRET}
    github-secret: ${NOTIO_GITHUB_SECRET}
    internal-token: ${NOTIO_INTERNAL_TOKEN}
  rag:
    embedding-dimension: ${NOTIO_EMBED_DIM:768}
    top-k: ${NOTIO_RAG_TOP_K:5}
  redis:
    summary-ttl-hours: 24
```

---

## 8. MVP 체크리스트

### 환경 세팅
- [ ] Spring Boot 4.x 프로젝트 생성 (Gradle Kotlin DSL)
- [ ] Java 25 설정 (`toolchain { languageVersion = JavaLanguageVersion.of(25) }`)
- [ ] 패키지 구조 생성 (`com.notio.*`)
- [ ] `application.yml` / `application-local.yml` 분리
- [ ] Flyway 마이그레이션 설정
- [ ] Docker Compose에서 PostgreSQL + Redis + Ollama 연결 확인
- [ ] SpringDoc OpenAPI (Swagger UI) 설정
- [ ] `ApiResponse<T>` 공통 응답 래퍼 구현
- [ ] `GlobalExceptionHandler` + `ErrorCode` 구현

### Webhook 수신
- [ ] `WebhookHandler` 인터페이스 정의
- [ ] `WebhookVerifier` 인터페이스 정의 (전략 패턴)
- [ ] `WebhookDispatcher` 구현 (Handler 자동 탐색)
- [ ] `ClaudeHookHandler` — Bearer 토큰 검증
- [ ] `SlackWebhookHandler` — HMAC-SHA256 검증
- [ ] `GithubWebhookHandler` — HMAC-SHA256 검증
- [ ] `NotificationEvent` DTO 정의
- [ ] `WebhookController` — `POST /api/v1/webhook/{source}`
- [ ] 잘못된 서명 → 401 응답 확인

### 알림 도메인
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

### 푸시 발송
- [ ] `Device` 엔티티 + `V3__create_devices.sql`
- [ ] Firebase Admin SDK 의존성 추가 + 설정
- [ ] `PushService.sendPush(notificationId)` 구현
- [ ] `POST /api/v1/devices/register` — FCM 토큰 등록
- [ ] 알림 저장 후 FCM 푸시 발송 연동 (동기)
- [ ] Android 실기기 푸시 수신 확인

### AI 채팅 (Spring AI + Ollama)
- [ ] Spring AI Ollama 의존성 + `SpringAiConfig` 설정
- [ ] `ChatService.chat(ChatRequest)` — RAG 포함 채팅
- [ ] `ChatService.streamChat(ChatRequest)` — SSE 스트리밍
- [ ] `DailySummaryService.getSummary()` — Redis 캐시 24h
- [ ] `ChatController`
  - [ ] `POST /api/v1/chat` — 단건 응답
  - [ ] `GET  /api/v1/chat/stream` — SSE 스트리밍
  - [ ] `GET  /api/v1/chat/daily-summary` — 오늘 요약
  - [ ] `GET  /api/v1/chat/history` — 채팅 이력

### 할일
- [ ] `Todo` 엔티티 + `V2__create_todos.sql`
- [ ] `TodoStatus` enum (PENDING, IN_PROGRESS, DONE)
- [ ] `TodoService.createFromNotification(request)` — LLM 제목 자동 생성
- [ ] `TodoController`
  - [ ] `POST  /api/v1/todos` — 생성
  - [ ] `GET   /api/v1/todos` — 목록
  - [ ] `PATCH /api/v1/todos/{id}` — 상태 변경

### 분석
- [ ] `AnalyticsService.getWeeklySummary()` 구현
- [ ] `GET /api/v1/analytics/weekly` — 주간 통계

### 테스트
- [ ] `NotificationServiceTest` 단위 테스트
- [ ] `WebhookDispatcherTest` 단위 테스트
- [ ] `NotificationControllerTest` 슬라이스 테스트
- [ ] `WebhookControllerTest` — 서명 검증 시나리오
- [ ] Testcontainers PostgreSQL + Redis 통합 테스트 환경 구성

### 코드 품질
- [ ] Checkstyle 설정 (`google_checks.xml` 기반)
- [ ] SpotBugs 설정
- [ ] `.editorconfig` 설정
- [ ] GitHub Actions `ci-backend.yml` — `backend/**` 변경 시만 실행
