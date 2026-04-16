# Backend Fix 개발 체크리스트

> Spring Boot 4.x · Java 25 · Connections 기반 Webhook/Notification 보안 수정

---

## Phase 0. Notification 사용자 격리 긴급 수정

> 목표: 멀티 유저 환경에서 사용자가 본인 알림만 조회, 수정, 삭제할 수 있도록 Notification 경계를 우선 보강
> 기준: Phase 0 local/dev 데이터는 기본 테스트 사용자로 backfill한다. 운영 데이터가 있는 환경은 이 migration 적용 전 별도 사용자 매핑 migration으로 교체한다.

### 데이터베이스

- [x] `notifications.connection_id` 컬럼 추가
- [x] `notifications.user_id` null 데이터 backfill 정책 확정
- [x] Phase 0 운영 데이터가 없으면 기존 null `user_id`를 기본 테스트 사용자로 backfill
- [x] 운영 데이터가 있으면 임의 사용자 매핑 없이 별도 backfill 절차 수립
- [x] `notifications.user_id`를 `NOT NULL`로 전환
- [x] `fk_notifications_users` 외래키 추가
- [x] `fk_notifications_connections` 외래키 추가
- [x] `idx_notifications_user_created_at` 인덱스 추가
- [x] `idx_notifications_user_read` 인덱스 추가
- [x] `idx_notifications_user_source` 인덱스 추가
- [x] `idx_notifications_user_connection` 인덱스 추가
- [x] 모든 schema 변경은 Flyway migration으로 작성

### Notification API 사용자 격리

- [x] `NotificationController`에서 모든 요청의 로그인 사용자 id 추출
- [x] `NotificationService.findAll(userId, source, isRead, pageable)`로 시그니처 변경
- [x] `NotificationService.findById(userId, id)`로 시그니처 변경
- [x] `NotificationService.markRead(userId, id)`로 시그니처 변경
- [x] `NotificationService.markAllRead(userId)`로 시그니처 변경
- [x] `NotificationService.delete(userId, id)`로 시그니처 변경
- [x] `NotificationService.countUnread(userId)`로 시그니처 변경
- [x] Repository 조회 조건에 항상 `user_id = :userId` 포함
- [x] 다른 사용자 알림 id 접근 시 `403`이 아니라 `404 NOTIFICATION_NOT_FOUND` 반환
- [x] `read-all`은 요청 사용자 알림만 변경
- [x] 삭제는 기존 soft delete 정책 유지

### Webhook 저장 경계

- [x] `NotificationService.saveFromConnection(event, connection)` 추가
- [x] Webhook 저장 시 `userId = connection.userId` 설정
- [x] Webhook 저장 시 `connectionId = connection.id` 설정
- [x] 기존 `saveFromEvent(NotificationEvent)` 사용 지점 점검
- [x] 사용자 식별 없이 저장되는 webhook 경로 제거 또는 deprecated 처리

### Redis 캐시

- [x] unread count cache key를 `unreadCount::{userId}` 형식으로 변경
- [x] 알림 생성, 읽음, 전체 읽음, 삭제 시 사용자별 cache invalidation 적용
- [x] 기존 전역 unread count cache key 사용 지점 제거

---

## Phase 1. Connections 통합 모델 도입

> 목표: Claude, Slack, Gmail 및 향후 provider를 하나의 Connection 모델로 관리

### 데이터베이스

- [x] `connections` 테이블 Flyway migration 작성
- [x] `connection_credentials` 테이블 Flyway migration 작성
- [x] `connection_events` 테이블 Flyway migration 작성
- [x] `connections.user_id`에 `fk_connections_users` 외래키 추가
- [x] `connection_credentials.connection_id`에 `fk_connection_credentials_connections` 외래키 추가
- [x] `idx_connections_user_id` 인덱스 추가
- [x] `idx_connections_provider` 인덱스 추가
- [x] `idx_connections_status` 인덱스 추가
- [x] `idx_connections_external_account` 인덱스 추가
- [x] `idx_connections_external_workspace` 인덱스 추가
- [x] `idx_connections_subscription` 인덱스 추가
- [x] active credential의 `key_prefix` unique partial index 추가
- [x] connection event 조회용 인덱스 추가

### Domain 모델

- [x] `com.notio.connection` 도메인 패키지 생성
- [x] `Connection` 엔티티 정의
- [x] `ConnectionCredential` 엔티티 정의
- [x] `ConnectionEvent` 엔티티 정의
- [x] `ConnectionProvider` enum 정의 (`CLAUDE`, `SLACK`, `GMAIL`, `GITHUB`, `DISCORD`, `JIRA`, `LINEAR`, `TEAMS`)
- [x] `ConnectionAuthType` enum 정의 (`API_KEY`, `OAUTH`, `SIGNATURE`, `SYSTEM`)
- [x] `ConnectionStatus` enum 정의 (`PENDING`, `ACTIVE`, `NEEDS_ACTION`, `REVOKED`, `ERROR`)
- [x] `ConnectionCapability` enum 정의 (`WEBHOOK_RECEIVE`, `TEST_MESSAGE`, `REFRESH_TOKEN`, `ROTATE_KEY`)
- [x] `ConnectionRepository` 구현
- [x] `ConnectionCredentialRepository` 구현
- [x] `ConnectionEventRepository` 구현
- [x] soft delete 조건을 repository query에 일관 적용

### Connection 관리 API

- [x] `ConnectionController` 구현
- [x] `ConnectionService` 구현
- [x] `GET /api/v1/connections` 구현
- [x] `GET /api/v1/connections/{id}` 구현
- [x] `POST /api/v1/connections` 구현
- [x] `DELETE /api/v1/connections/{id}` 구현
- [x] `POST /api/v1/connections/{id}/test` 구현
- [x] `POST /api/v1/connections/{id}/refresh` 구현
- [x] `POST /api/v1/connections/{id}/rotate-key` 구현
- [x] 모든 connection 조회는 로그인 사용자 기준으로 제한
- [x] 다른 사용자 connection 접근 시 `404 CONNECTION_NOT_FOUND` 반환
- [x] 목록/상세 응답에 원문 API Key가 포함되지 않도록 검증
- [x] API Key 생성 응답에서만 원문 key 1회 반환
- [x] API Key rotate 응답에서만 새 원문 key 1회 반환
- [x] 삭제는 soft delete 또는 credential revoke 정책으로 처리

### 감사 로그

- [x] connection 생성 event 기록
- [x] OAuth 시작 event 기록
- [x] OAuth 성공 event 기록
- [x] OAuth 실패 event 기록
- [x] API Key rotate event 기록
- [x] API Key revoke event 기록
- [x] webhook success/failure event 기록
- [x] rate limit hit event 기록
- [x] provider token refresh 실패 event 기록

---

## Phase 2. Webhook 인증 구조 개편

> 목표: 전역 bearer token을 제거하고 connection 단위 인증, 폐기, 감사가 가능한 구조로 전환

### 인증 경계

- [x] 일반 앱 API는 기존 JWT 인증 유지
- [x] `/api/v1/connections/**`는 JWT 인증 필요
- [x] `/api/v1/webhook/**`는 Spring Security에서 `permitAll` 유지
- [x] `JwtAuthenticationFilter.shouldNotFilter()`에서 `/api/v1/webhook/` skip
- [x] Webhook 실제 인증은 receive service와 provider adapter에서 수행
- [x] 기존 `NOTIO_WEBHOOK_CLAUDE_TOKEN` 사용 경로 제거 또는 deprecated 처리

### Opaque Webhook API Key

- [x] `ApiKeyGenerator` 구현
- [x] API Key 형식을 `ntio_wh_<prefix>_<secret>`로 고정
- [x] prefix는 8~12 bytes CSPRNG base64url no-padding으로 생성
- [x] secret은 최소 32 bytes CSPRNG base64url no-padding으로 생성
- [x] `ConnectionCredentialHasher` 구현
- [x] `NOTIO_WEBHOOK_KEY_PEPPER` 기반 HMAC-SHA256 hash 저장
- [x] constant-time compare 적용
- [x] `key_prefix`, `key_preview`, `key_hash` 저장
- [x] API Key 원문은 DB에 저장하지 않음
- [x] revoked credential 거부
- [x] expired credential 거부
- [x] deleted credential 거부
- [x] provider mismatch 거부
- [x] inactive/deleted connection 거부
- [x] inactive user 거부
- [x] 검증 성공 시 `WebhookPrincipal(connectionId, userId, provider)` 반환

### Credential 암호화

- [x] `CredentialEncryptionService` 구현
- [x] OAuth access token 암호화 저장
- [x] OAuth refresh token 암호화 저장
- [x] `NOTIO_CREDENTIAL_ENCRYPTION_KEY` 환경변수 사용
- [x] 암호화 키 누락 시 애플리케이션 시작 실패 또는 profile별 명확한 실패 정책 적용

### Provider Adapter

- [x] `ConnectionProviderAdapter` interface 정의
- [x] `ConnectionProviderAdapterRegistry` 구현
- [x] provider 미지원 시 `CONNECTION_PROVIDER_UNSUPPORTED` 반환
- [x] auth type 미지원 시 `CONNECTION_AUTH_TYPE_UNSUPPORTED` 반환
- [x] `ClaudeConnectionAdapter` 구현
- [x] `SlackConnectionAdapter` skeleton 구현
- [x] `GmailConnectionAdapter` skeleton 구현
- [x] 향후 provider 추가가 adapter 추가 중심으로 가능하도록 분기 격리

### Claude Webhook

- [x] Claude connection 생성 시 API Key credential 생성
- [x] Claude webhook 요청에서 Notio API Key 검증
- [x] Claude payload를 Notification event로 변환
- [x] Claude notification 저장 시 connection의 user id 사용
- [x] Claude hook script 환경변수를 `NOTIO_WEBHOOK_API_KEY`로 문서화

### Slack OAuth/Webhook

- [x] Slack OAuth URL 생성 skeleton 구현
- [x] Slack OAuth callback 처리 skeleton 구현
- [x] Slack signing secret 검증 skeleton 구현
- [x] Slack URL verification challenge 처리 skeleton 구현
- [x] `team_id`로 connection 매칭하도록 설계
- [x] payload 문자열만으로 사용자를 매칭하지 않음

### Gmail OAuth/Webhook

- [x] Google OAuth URL 생성 skeleton 구현
- [x] Google OAuth callback 처리 skeleton 구현
- [x] provider account id/email 저장 skeleton 구현
- [x] Pub/Sub subscription 생성/갱신 skeleton 구현
- [x] Google Pub/Sub/OIDC/subscription 검증 skeleton 구현
- [x] `subscription_id` 또는 provider account id로 connection 매칭하도록 설계
- [x] payload email 문자열만으로 사용자를 매칭하지 않음

---

## Phase 3. Rate Limit 및 Payload 제한

> 목표: Webhook뿐 아니라 모든 `/api/v1/**`에 공통 방어선을 적용

### Rate Limit 구조

- [x] `RateLimitFilter` 또는 `HandlerInterceptor` 구현
- [x] `RateLimitService` 구현
- [x] `RateLimitPolicyResolver` 구현
- [x] `RedisRateLimitStore` 구현
- [x] `/api/v1/**` 전체에 rate limit 적용
- [x] `/swagger-ui/**` 제외
- [x] `/api-docs/**` 제외
- [x] health check 제외
- [x] static assets 제외
- [x] Redis 장애 시 local/dev profile fail-open 정책 적용
- [x] 운영 profile에서 민감 endpoint fail-closed 옵션 제공

### Rate Limit 정책

- [x] login은 IP당 5회/분, 30회/시간 제한
- [x] refresh는 IP당 30회/분, user당 60회/시간 제한
- [x] webhook은 connection/key prefix당 30회/분, 5,000회/일 제한
- [x] webhook은 IP당 60회/분 보조 제한
- [x] notifications-read는 user당 120회/분 제한
- [x] notifications-write는 user당 60회/분 제한
- [x] chat-ai는 user당 20회/분, 200회/일 제한
- [x] device-register는 user당 10회/분, IP당 30회/분 제한
- [x] 잘못된 webhook key 반복 요청도 IP 기준으로 제한

### Rate Limit 응답

- [x] 초과 시 `429 RATE_LIMIT_EXCEEDED` 반환
- [x] `Retry-After` header 포함
- [x] `X-RateLimit-Limit` header 포함
- [x] `X-RateLimit-Remaining` header 포함
- [x] `X-RateLimit-Reset` header 포함
- [x] 응답 body는 `ApiResponse` error 형식 유지

### Payload 제한

- [x] Webhook body 64KB 초과 시 JSON parse 전에 `413` 반환
- [x] 일반 JSON body 1MB 초과 시 JSON parse 전에 `413` 반환
- [x] Chat input 글자 수 제한 적용
- [x] 인증, rate limit, payload 제한 이후에만 비싼 작업 수행

---

## Phase 4. Backend 테스트 및 검증

> 목표: 사용자 격리, connection 단위 인증, rate limit 회귀를 자동화

### Connection 테스트

- [x] 사용자는 본인 connection만 조회 가능
- [x] 다른 사용자 connection id 접근 시 404 반환
- [x] API Key 생성 응답에만 원문 key 포함
- [x] API Key rotate 응답에만 새 원문 key 포함
- [x] 목록/상세 응답에 원문 key 미포함
- [x] DB에 원문 API Key 미저장 검증
- [x] connection 삭제 시 soft delete 또는 revoke 처리 검증

### Webhook 테스트

- [x] Claude API Key로 connection과 user를 식별해 notification 저장
- [x] revoked key는 401
- [x] expired key는 401
- [x] malformed key는 401
- [x] provider mismatch는 401
- [x] Slack signature 실패 시 notification 미저장
- [x] Gmail provider 검증 실패 시 notification 미저장
- [x] webhook 성공 시 `connection.last_used_at` 갱신
- [x] webhook success/failure event 기록

### Notification 테스트

- [x] 사용자 A는 사용자 A 알림만 목록에서 조회
- [x] 사용자 A가 사용자 B 알림 상세 요청 시 404
- [x] 사용자 A가 사용자 B 알림 읽음 처리 시 404
- [x] `read-all`은 요청 사용자 알림만 변경
- [x] unread count는 사용자별 분리
- [x] unread count cache key는 사용자별 분리
- [x] webhook 저장 알림에 `user_id`와 `connection_id` 저장

### Rate Limit 테스트

- [x] login 반복 실패는 IP 기준으로 429
- [x] webhook 반복 요청은 key prefix 기준으로 429
- [x] webhook 반복 요청은 IP 기준으로 429
- [x] 잘못된 webhook key 반복 요청은 IP 기준으로 429
- [x] notification read/write 정책이 분리 적용
- [x] chat/AI API는 낮은 quota 적용

### 품질 확인

- [x] `./gradlew test` 통과
- [x] Checkstyle 설정이 있으면 `./gradlew checkstyleMain` 통과
- [x] SpotBugs 설정이 있으면 `./gradlew spotbugsMain` 통과
- [x] Swagger/OpenAPI 문서에서 신규 endpoint 확인
- [x] local profile에서 애플리케이션 기동 확인
