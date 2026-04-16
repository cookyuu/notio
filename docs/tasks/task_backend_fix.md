# Backend Fix 개발 체크리스트

> 대상: `docs/plans/plan_fix.md` 기반 인증 확장 구현  
> 범위: Spring Boot 4.x · Java 25 · 이메일 인증 + 계정 복구 + OAuth 확장 구조

---

## Phase 0. 인증 도메인 재구성

- [x] `users` 구조를 `primary_email`, `display_name`, `status`, `deleted_at` 중심으로 정리한다.
- [x] `auth_identities` 테이블을 추가하고 `provider`, `provider_user_id`, `email`, `password_hash`, `email_verified`를 설계한다.
- [x] `password_reset_tokens` 테이블을 추가하고 `token_hash`, `expires_at`, `used_at` 정책을 반영한다.
- [x] `auth_provider_states` 테이블을 추가하고 `provider`, `state`, `platform`, `redirect_uri`, `pkce_verifier`, `expires_at`를 저장할 수 있게 한다.
- [x] `auth_identities`에 LOCAL 이메일 unique, OAuth provider/provider_user_id unique 제약을 설정한다.
- [x] 관련 FK, 인덱스, 소프트 삭제 컬럼 정책을 Flyway 마이그레이션으로 반영한다.
- [x] 기존 `users.email/password` 직접 참조 로직을 `User + AuthIdentity` 구조로 옮길 영향 범위를 점검한다.

## Phase 1. 로컬 인증 기능 구현

- [ ] `POST /api/v1/auth/signup` 요청/응답 DTO를 추가한다.
- [ ] `POST /api/v1/auth/find-id` 요청/응답 DTO를 추가한다.
- [ ] `POST /api/v1/auth/password-reset/request` 요청/응답 DTO를 추가한다.
- [ ] `POST /api/v1/auth/password-reset/confirm` 요청/응답 DTO를 추가한다.
- [ ] `LocalAuthService`를 추가하거나 기존 인증 서비스에서 로컬 인증 책임을 분리한다.
- [ ] 회원가입 시 이메일 중복 검사, 비밀번호 해시 저장, `User + LOCAL AuthIdentity` 생성을 구현한다.
- [ ] 아이디 찾기 요청 시 계정 존재 여부를 노출하지 않는 공통 성공 응답 정책을 구현한다.
- [ ] 비밀번호 재설정 요청 시 raw token 발급 후 DB에는 hash만 저장하도록 구현한다.
- [ ] 비밀번호 재설정 확정 시 token 유효성, 만료, 사용 여부를 검증하도록 구현한다.
- [ ] 비밀번호 재설정 성공 시 기존 refresh token을 모두 revoke하도록 구현한다.
- [ ] 기존 로그인 로직이 `LOCAL AuthIdentity` 기준으로 인증하도록 정리한다.
- [ ] 신규 인증 API를 `AuthController` 또는 인증 전용 컨트롤러에 노출한다.

## Phase 2. OAuth 확장 뼈대 구현

- [ ] `AuthProvider` enum에 `LOCAL`, `GOOGLE`, `APPLE`, `KAKAO`, `NAVER`를 정의한다.
- [ ] `AuthPlatform` enum에 모바일/웹 구분값을 정의한다.
- [ ] `AuthProviderAdapter` 인터페이스를 추가한다.
- [ ] provider adapter registry 또는 resolver를 추가한다.
- [ ] `OAuthAuthService`를 추가하고 OAuth 시작/콜백/교환 흐름의 공통 책임을 모은다.
- [ ] `POST /api/v1/auth/oauth/start` 요청/응답 DTO와 컨트롤러를 추가한다.
- [ ] `GET /api/v1/auth/oauth/callback/{provider}` 엔드포인트와 state 검증 흐름을 추가한다.
- [ ] `POST /api/v1/auth/oauth/exchange` 요청/응답 DTO와 컨트롤러를 추가한다.
- [ ] 미지원 provider, 잘못된 callback/state, provider별 오류를 표준 에러 코드로 변환한다.
- [ ] 실제 Google/Apple/Kakao/Naver 연동 전까지는 공통 registry/adapter 뼈대와 unsupported 처리까지 구현한다.

## Phase 3. 보안 및 운영 정책 반영

- [ ] `SecurityConfig`에 `signup`, `find-id`, `password-reset/**`, `oauth/start`, `oauth/callback/**`, `oauth/exchange`를 `permitAll`로 반영한다.
- [ ] `EMAIL_ALREADY_EXISTS`, `PASSWORD_RESET_TOKEN_INVALID`, `PASSWORD_RESET_TOKEN_EXPIRED`, `AUTH_PROVIDER_UNSUPPORTED`, `OAUTH_STATE_INVALID`, `OAUTH_CALLBACK_FAILED` 등을 에러 코드에 추가한다.
- [ ] 인증 관련 요청 body 검증 규칙을 추가한다.
- [ ] 비밀번호 최소 길이, 이메일 형식 검증을 backend에서 강제한다.
- [ ] `AuthMailSender` 인터페이스를 추가한다.
- [ ] `local/dev` 프로필용 fake/log mail sender 구현을 추가한다.
- [ ] 아이디 찾기/비밀번호 찾기 메일 템플릿 또는 메시지 생성 책임을 정리한다.
- [ ] 신규 인증 엔드포인트에 대한 rate limit 정책을 추가한다.
- [ ] 감사 로그/audit 이벤트에 signup, password reset, oauth start/callback 결과를 기록한다.
- [ ] password, raw reset token, provider token이 로그에 남지 않도록 마스킹 정책을 점검한다.
- [ ] OpenAPI 또는 `/api-docs`에 신규 인증 API가 노출되도록 정리한다.

## Phase 4. 설정 및 환경 변수 정리

- [ ] `application.yml`에 인증 확장 관련 기본 설정을 추가한다.
- [ ] 메일 발송, reset token TTL, OAuth redirect/state 만료 설정을 구성 가능하게 만든다.
- [ ] OAuth provider별 client 설정 키를 `NOTIO_` prefix 환경 변수 기준으로 정리한다.
- [ ] `local`, `dev`, `prod` 프로필별 동작 차이를 문서화 가능한 형태로 구성한다.

## Phase 5. 테스트 및 검증

- [ ] 회원가입 성공 케이스 테스트를 추가한다.
- [ ] 중복 이메일 회원가입 실패 테스트를 추가한다.
- [ ] 아이디 찾기 요청이 계정 존재 여부와 무관하게 동일 응답을 반환하는지 테스트한다.
- [ ] 비밀번호 재설정 요청이 계정 존재 여부와 무관하게 동일 응답을 반환하는지 테스트한다.
- [ ] 비밀번호 재설정 token 만료/재사용/위조 실패 테스트를 추가한다.
- [ ] 비밀번호 재설정 성공 후 refresh token revoke 테스트를 추가한다.
- [ ] `SecurityConfig`에서 신규 public auth endpoint 접근 가능 여부를 테스트한다.
- [ ] OAuth provider registry가 미지원 provider를 올바르게 거부하는지 테스트한다.
- [ ] fake/log mail sender 호출 테스트를 추가한다.
- [ ] `./gradlew test` 또는 `gradlew.bat test`를 통과시킨다.
- [ ] 품질 검사 태스크가 있으면 `checkstyleMain`, `spotbugsMain`까지 확인한다.
- [ ] 애플리케이션 기동 후 `/api-docs` 또는 `swagger-ui`에서 신규 명세 노출을 확인한다.
