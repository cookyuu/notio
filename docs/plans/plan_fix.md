# 인증 확장 설계 계획

> 대상: 로그인/회원가입/아이디 찾기/비밀번호 찾기 + 향후 Kakao/Naver/Apple/Google OAuth 로그인까지 고려한 확장형 인증 구조

---

## 1. 목표

현재 시스템은 이메일/비밀번호 로그인만 지원한다.  
회원가입, 아이디 찾기, 비밀번호 찾기 화면 및 기능은 없고, 향후 소셜 OAuth 로그인도 추가할 계획이다.

이번 설계의 목표는 다음과 같다.

- 로그인 화면에서 회원가입, 아이디 찾기, 비밀번호 찾기 화면으로 이동 가능해야 한다.
- 회원가입, 아이디 찾기, 비밀번호 찾기 화면에서도 로그인 화면으로 돌아갈 수 있어야 한다.
- 회원가입, 아이디 찾기, 비밀번호 찾기 기능을 실제 동작 가능한 구조로 설계해야 한다.
- 이후 Kakao, Naver, Apple, Google OAuth 로그인을 무리 없이 붙일 수 있도록 인증 구조를 처음부터 확장성 있게 설계해야 한다.
- 모바일과 웹을 모두 고려한 인증 구조여야 한다.

---

## 2. 핵심 설계 원칙

### 2.1 로그인 식별자 정책
- 로그인 식별자는 `이메일 = 아이디`로 정의한다.
- 별도의 username/loginId 컬럼은 도입하지 않는다.
- 따라서 "아이디 찾기"는 별도 아이디를 복구하는 기능이 아니라, 가입 이메일 안내형 기능으로 정의한다.

### 2.2 인증 구조 분리
인증 구조는 `사용자(User)`와 `로그인 수단(AuthIdentity)`를 분리한다.

- `User`: 앱 내부 사용자
- `AuthIdentity`: 로그인 수단별 인증 정보
  - LOCAL (이메일/비밀번호)
  - GOOGLE
  - APPLE
  - KAKAO
  - NAVER

이 구조를 사용하면:
- 로컬 로그인과 소셜 로그인을 하나의 사용자에 연결할 수 있다.
- 이후 소셜 로그인 추가 시 users 테이블을 다시 뜯어고치지 않아도 된다.
- 계정 연결/해제 기능도 자연스럽게 확장할 수 있다.

### 2.3 플랫폼 범위
- 인증 설계는 `모바일 + 웹 공통` 기준으로 한다.
- 모바일은 custom scheme / universal link / app link를 사용한다.
- 웹은 HTTPS callback route를 사용한다.

### 2.4 보안 원칙
- 아이디 찾기, 비밀번호 찾기 요청에서 계정 존재 여부를 외부에 노출하지 않는다.
- password reset token은 raw 값이 아니라 hash만 저장한다.
- 비밀번호 변경 성공 시 기존 refresh token은 모두 revoke한다.
- OAuth state는 반드시 저장하고 검증한다.
- provider access token, password, raw reset token은 로그에 남기지 않는다.

---

## 3. 데이터 모델 설계

### 3.1 users
사용자 공통 정보를 저장한다.

권장 필드:
- `id`
- `primary_email`
- `display_name` nullable
- `status` (`ACTIVE`, `PENDING`, `LOCKED`, `DELETED`)
- `created_at`
- `updated_at`
- `deleted_at`

### 3.2 auth_identities
로그인 수단별 인증 정보를 저장한다.

필드:
- `id`
- `user_id`
- `provider` (`LOCAL`, `GOOGLE`, `APPLE`, `KAKAO`, `NAVER`)
- `provider_user_id` nullable for LOCAL
- `email`
- `password_hash` nullable for OAuth
- `email_verified`
- `created_at`
- `updated_at`
- `deleted_at`

제약:
- LOCAL: `(provider, email)` unique
- OAuth: `(provider, provider_user_id)` unique

### 3.3 refresh_tokens
기존 구조 유지  
단, 아래 이벤트에서 revoke:
- 비밀번호 재설정 성공
- 계정 보안상 강제 로그아웃 필요 시

### 3.4 password_reset_tokens
필드:
- `id`
- `user_id`
- `token_hash`
- `expires_at`
- `used_at`
- `created_at`

정책:
- raw token 미저장
- 1회 사용
- 짧은 TTL
- 성공 시 `used_at` 처리

### 3.5 auth_provider_states
OAuth state 및 callback 검증용 저장소

필드:
- `id`
- `provider`
- `state`
- `platform`
- `redirect_uri`
- `pkce_verifier` nullable
- `expires_at`
- `created_at`

용도:
- 모바일/웹 callback 공통 검증
- CSRF 및 callback 위변조 방지

---

## 4. 백엔드 인증 구조

### 4.1 서비스 분리
#### AuthSessionService
- JWT access token 발급
- refresh token 발급/재발급
- logout 처리

#### LocalAuthService
- 회원가입
- 이메일/비밀번호 로그인
- 아이디 찾기
- 비밀번호 재설정 요청
- 비밀번호 재설정 확정

#### OAuthAuthService
- OAuth 시작
- callback 처리
- provider profile 정규화
- 기존 사용자 연결 또는 신규 사용자 생성

#### AuthProviderAdapter
provider별 OAuth 처리 전담
- supports
- authorization URL 생성
- code/token 교환
- 외부 사용자 정보 조회
- profile normalize

#### AuthMailSender
- 아이디 찾기 메일
- 비밀번호 재설정 메일
- local/dev에서는 fake sender 또는 log sender 사용
- prod에서는 실제 메일 발송 구현 연결

---

## 5. 백엔드 API 설계

### 5.1 로컬 인증 API

#### POST /api/v1/auth/signup
요청:
- `email`
- `password`

동작:
- 이메일 중복 검사
- password hash 저장
- `User + LOCAL AuthIdentity` 생성

#### POST /api/v1/auth/login
요청:
- `email`
- `password`

동작:
- LOCAL identity 기준 로그인

#### POST /api/v1/auth/find-id
요청:
- `email`

동작:
- 계정이 존재하면 가입 이메일/로그인 안내 메일 발송
- 계정이 없어도 동일한 성공 응답 반환

#### POST /api/v1/auth/password-reset/request
요청:
- `email`

동작:
- 계정이 존재하면 reset token 생성 및 메일 발송
- 계정이 없어도 동일한 성공 응답 반환

#### POST /api/v1/auth/password-reset/confirm
요청:
- `token`
- `new_password`

동작:
- token 검증
- 비밀번호 변경
- token 사용 처리
- refresh token revoke

### 5.2 OAuth 공통 API

#### POST /api/v1/auth/oauth/start
요청:
- `provider`
- `platform`
- `redirect_uri`

응답:
- `authorization_url`
- `state`

#### GET /api/v1/auth/oauth/callback/{provider}
용도:
- 웹 callback 처리
- 서버 측 state 검증
- 최종적으로 frontend callback route로 전달

#### POST /api/v1/auth/oauth/exchange
용도:
- 모바일 앱 또는 웹 callback 화면이 provider 결과를 서버에 전달
- provider code/token을 앱 세션으로 교환

### 5.3 미래 확장 API
- `POST /api/v1/auth/oauth/link`
- `DELETE /api/v1/auth/oauth/link/{provider}`

---

## 6. 프론트엔드 화면 및 라우팅 설계

### 6.1 라우트
- `/login`
- `/signup`
- `/find-id`
- `/reset-password/request`
- `/reset-password/confirm`
- `/auth/oauth/callback`

### 6.2 로그인 화면
구성:
- 이메일 입력
- 비밀번호 입력
- 로그인 버튼
- 이동 버튼
  - 회원가입
  - 아이디 찾기
  - 비밀번호 찾기
- 소셜 로그인 버튼 영역
  - Google
  - Apple
  - Kakao
  - Naver

설계 원칙:
- 소셜 버튼은 config 기반 렌더링
- 활성화되지 않은 provider는 숨길 수 있도록 설계

### 6.3 회원가입 화면
입력:
- 이메일
- 비밀번호
- 비밀번호 확인

기능:
- 유효성 검증
- 회원가입 요청
- 성공 시 로그인 화면으로 이동

### 6.4 아이디 찾기 화면
입력:
- 이메일

기능:
- 가입 이메일 안내 메일 요청
- 성공 메시지 표시
- 로그인 화면으로 이동 가능

### 6.5 비밀번호 찾기 화면
#### 요청 화면
입력:
- 이메일

기능:
- 재설정 메일 요청

#### 확정 화면
입력:
- token
- 새 비밀번호
- 비밀번호 확인

기능:
- 새 비밀번호 저장
- 성공 시 로그인 화면 이동

### 6.6 OAuth callback 화면
용도:
- 웹 callback route 처리
- 모바일 deep link 진입 처리
- code/state/token 파싱 후 backend exchange API 호출

---

## 7. 프론트엔드 상태 관리 설계

기존 `AuthNotifier`는 세션 상태와 로그인 액션이 혼합되어 있다.  
확장성을 위해 다음과 같이 분리한다.

### 7.1 AuthSessionNotifier
역할:
- 로그인 여부
- 현재 사용자 이메일
- refresh
- logout

### 7.2 액션 provider 분리
- `loginActionProvider`
- `signupActionProvider`
- `findIdActionProvider`
- `passwordResetRequestProvider`
- `passwordResetConfirmProvider`
- `socialLoginActionProvider`

이 구조를 선택하는 이유:
- 화면별 로딩/에러 충돌 방지
- notifier 비대화 방지
- 소셜 로그인 추가 시 영향 최소화

---

## 8. OAuth 확장성 설계

### 8.1 AuthProvider enum
- LOCAL
- GOOGLE
- APPLE
- KAKAO
- NAVER

### 8.2 provider adapter 계약
각 provider adapter는 아래를 구현한다.
- `supports()`
- `buildAuthorizationUrl(...)`
- `exchangeCode(...)`
- `fetchUserProfile(...)`
- `normalizeProfile(...)`

### 8.3 정규화된 프로필 구조
- `provider`
- `providerUserId`
- `email`
- `emailVerified`
- `displayName`
- `rawProfile` optional

### 8.4 provider별 설계 고려사항
#### Google
- OIDC 기반
- 이메일/검증 상태 활용 가능

#### Apple
- 최초 로그인 시만 이름/이메일 일부 제공 가능
- `providerUserId`를 핵심 식별자로 사용해야 함
- 이메일 부재 가능성 고려

#### Kakao
- 이메일 제공이 optional일 수 있음
- 이메일 미제공 시 후속 onboarding 가능성 고려

#### Naver
- profile 응답 구조 normalize 필요

### 8.5 계정 병합 정책
자동 병합은 기본적으로 하지 않는다.

원칙:
- 동일 provider + 동일 providerUserId면 동일 identity
- 이메일이 같아도 자동 병합하지 않음
- 계정 연결은 추후 명시적 기능으로 처리

이유:
- 계정 탈취 및 잘못된 병합 리스크가 큼

---

## 9. 플랫폼 설정 설계

### 9.1 Android
추가 필요:
- OAuth callback용 intent-filter
- custom scheme 예: `notio://auth/oauth/callback`
- 향후 app links 확장 가능 구조

### 9.2 iOS
추가 필요:
- `CFBundleURLTypes`
- 필요 시 Associated Domains
- universal link 대응

### 9.3 Web
추가 필요:
- `/auth/oauth/callback` route 처리
- redirect 기반 OAuth callback 페이지 설계
- base href 고려

---

## 10. 보안/운영 정책

### 10.1 계정 존재 여부 비노출
다음 API는 동일 응답을 반환한다.
- `find-id`
- `password-reset/request`

### 10.2 비밀번호 정책
- 최소 8자
- UI와 backend 모두 검증

### 10.3 토큰 처리
- password reset token은 hash 저장
- 비밀번호 재설정 성공 시 모든 refresh token revoke

### 10.4 Rate Limit 확장
다음 인증 endpoint에 별도 정책 추가
- `auth-signup`
- `auth-find-id`
- `auth-password-reset-request`
- `auth-password-reset-confirm`
- `auth-oauth-start`
- `auth-oauth-exchange`

### 10.5 로깅/감사
기록 대상:
- signup success/failure
- password reset request/confirm
- oauth start/callback success/failure

마스킹 대상:
- password
- raw token
- provider access token
- provider id token

---

## 11. 테스트 계획

### 11.1 프론트엔드
- 로그인 화면에서 회원가입/아이디 찾기/비밀번호 찾기 이동 가능
- 각 화면에서 로그인으로 복귀 가능
- 회원가입 유효성 검증
- 비밀번호 재설정 요청/확정 화면 검증
- 비인증 허용 라우트가 auth guard에 막히지 않는지 확인
- 소셜 로그인 버튼 노출/숨김 정책 검증
- callback route가 query/code/state를 정상 처리하는지 검증

### 11.2 백엔드
- 회원가입 성공
- 중복 이메일 회원가입 실패
- LOCAL identity 기반 로그인 성공/실패
- 아이디 찾기 요청 계정 존재 여부 비노출
- 비밀번호 재설정 요청 계정 존재 여부 비노출
- reset token 만료/재사용/위조 실패
- 비밀번호 재설정 성공 후 기존 refresh token 무효화
- auth identity 저장/조회 테스트
- oauth state 저장/검증 테스트
- unsupported provider 거부 테스트
- provider adapter registry 테스트

### 11.3 문서/계약
- OpenAPI에 신규 auth endpoint 반영
- redirect URI 정책 문서화
- 환경변수 목록 문서화

---

## 12. 구현 범위 정리

### 이번 단계 구현 대상
- 회원가입 화면/기능
- 아이디 찾기 화면/기능
- 비밀번호 찾기 화면/기능
- 로그인 화면 이동 버튼 추가
- 라우팅 확장
- LOCAL 인증 구조를 `User + AuthIdentity` 기반으로 전환
- OAuth 확장형 백엔드/프론트 구조의 뼈대 마련

### 다음 단계 구현 대상
- Google OAuth 실제 연동
- Apple OAuth 실제 연동
- Kakao OAuth 실제 연동
- Naver OAuth 실제 연동
- 계정 연결/해제 기능
- 필요 시 이메일 인증 회원가입

---

## 13. 최종 확정 사항

- 로그인 아이디는 이메일이다.
- 별도 username/loginId는 도입하지 않는다.
- 아이디 찾기는 가입 이메일 안내형 기능으로 정의한다.
- 비밀번호 찾기는 이메일 링크 기반 재설정 방식으로 정의한다.
- 인증 구조는 `User`와 `AuthIdentity`를 분리한다.
- OAuth는 provider registry + adapter 방식으로 확장한다.
- 플랫폼은 모바일과 웹을 모두 고려한다.
- 계정 자동 병합은 기본적으로 하지 않는다.
