# Frontend Fix 개발 체크리스트

> 대상: `docs/plans/plan_fix.md` 기반 인증 UI/플로우 확장 구현  
> 범위: Flutter 3.x · Dart 3.6 · 이메일 인증 + 계정 복구 + OAuth 확장 준비

---

## Phase 0. 인증 계약 및 타입 정리

- [x] 신규 auth API 계약에 맞는 request/response model을 추가한다.
- [x] `AuthProvider` enum에 `local`, `google`, `apple`, `kakao`, `naver`를 정의한다.
- [x] `AuthPlatform` 또는 플랫폼 전달 타입을 정의한다.
- [x] signup, find-id, password-reset, oauth start/exchange 관련 DTO를 정리한다.
- [x] backend 에러 코드와 매핑되는 프론트 메시지 처리 기준을 정리한다.

## Phase 1. 인증 데이터 레이어 확장

- [x] auth remote datasource에 `signup`, `findId`, `requestPasswordReset`, `confirmPasswordReset` 메서드를 추가한다.
- [x] auth remote datasource에 `startSocialLogin`, `exchangeSocialLogin` 메서드를 추가한다.
- [x] auth repository interface에 신규 인증 액션 계약을 추가한다.
- [x] auth repository 구현체에 신규 API 호출과 응답 매핑을 추가한다.
- [x] 기존 로그인/리프레시 저장소 로직이 새 응답 구조와 충돌하지 않는지 점검한다.
- [x] 네트워크 에러와 도메인 에러를 화면에서 다룰 수 있게 예외 변환을 정리한다.

## Phase 2. 라우팅 및 상태 관리 재구성

- [x] 라우트에 `/signup`, `/find-id`, `/reset-password/request`, `/reset-password/confirm`, `/auth/oauth/callback`을 추가한다.
- [x] auth guard 또는 `GoRouter` redirect 로직에 신규 비인증 허용 경로를 반영한다.
- [x] 기존 `AuthNotifier`에서 세션 상태와 액션 책임을 분리한다.
- [x] `AuthSessionNotifier` 또는 동등한 세션 전용 상태 관리 구조를 정리한다.
- [x] `loginActionProvider`를 분리 또는 정리한다.
- [x] `signupActionProvider`를 추가한다.
- [x] `findIdActionProvider`를 추가한다.
- [x] `passwordResetRequestProvider`를 추가한다.
- [x] `passwordResetConfirmProvider`를 추가한다.
- [x] `socialLoginActionProvider`를 추가한다.

## Phase 3. 인증 화면 구현

- [x] 로그인 화면에 `회원가입`, `아이디 찾기`, `비밀번호 찾기` 이동 버튼을 추가한다.
- [x] 로그인 화면에 소셜 로그인 버튼 영역을 추가한다.
- [x] 소셜 로그인 버튼이 provider config 기반으로 노출되도록 구현한다.
- [x] `SignupScreen`을 추가한다.
- [x] `FindIdScreen`을 추가한다.
- [x] `PasswordResetRequestScreen`을 추가한다.
- [x] `PasswordResetConfirmScreen`을 추가한다.
- [x] `AuthOAuthCallbackScreen`을 추가한다.
- [x] 회원가입, 아이디 찾기, 비밀번호 찾기 화면에 모두 `로그인으로 돌아가기` 버튼을 추가한다.
- [x] 각 화면에 로딩, 성공, 실패 상태를 일관된 UX로 반영한다.

## Phase 4. 입력 검증 및 UX 정리

- [x] 이메일 형식 검증을 추가한다.
- [x] 비밀번호 최소 길이 검증을 추가한다.
- [x] 회원가입/비밀번호 재설정에서 비밀번호 확인 일치 검증을 추가한다.
- [x] 중복 submit 방지 처리를 추가한다.
- [x] 계정 존재 여부를 노출하지 않는 find-id/reset-request 성공 메시지를 반영한다.
- [x] reset token을 query/path/deep link에서 읽어 처리할 수 있게 정리한다.
- [x] 로그인 상태에서 auth 화면 접근 시 redirect 정책을 정리한다.

## Phase 5. 플랫폼/OAuth 진입 준비

- [ ] Android OAuth callback용 intent-filter 또는 app link 구성을 반영한다.
- [ ] iOS URL scheme 또는 universal link 구성을 반영한다.
- [ ] Web `/auth/oauth/callback` 라우트와 redirect 흐름을 반영한다.
- [ ] provider별 client 설정 여부에 따라 버튼 노출을 제어하는 config 구조를 추가한다.
- [ ] 향후 Google/Apple/Kakao/Naver 실제 SDK 또는 redirect 기반 연동을 붙일 수 있게 진입 인터페이스를 정리한다.

## Phase 6. 테스트 및 검증

- [ ] 로그인 화면에서 회원가입/아이디 찾기/비밀번호 찾기 이동 버튼 노출 테스트를 추가한다.
- [ ] 각 인증 화면에서 로그인 복귀 동작 테스트를 추가한다.
- [ ] 회원가입 폼 유효성 검증 테스트를 추가한다.
- [ ] 아이디 찾기 요청 성공 상태 테스트를 추가한다.
- [ ] 비밀번호 재설정 요청/확정 화면 테스트를 추가한다.
- [ ] auth guard가 신규 비인증 라우트를 막지 않는지 테스트한다.
- [ ] 소셜 로그인 버튼 노출/숨김 정책 테스트를 추가한다.
- [ ] OAuth callback 화면이 code/state/token을 정상 파싱하는지 테스트한다.
- [x] `flutter analyze`를 통과시킨다.
- [ ] `flutter test`를 통과시킨다.
- [ ] 필요 시 `flutter build apk --debug` 또는 동등한 빌드 검증을 수행한다.
