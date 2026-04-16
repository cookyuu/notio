# Frontend Fix 개발 체크리스트

> Flutter 3.x · Dart 3.6 · Riverpod · Connections 기반 연동 관리 UI

---

## Phase 0. API 계약 반영

> 목표: `spec_fix.md` 기준으로 Connections와 보안 수정 API를 Flutter 타입으로 반영

### 타입 정의

- [x] `ConnectionProvider` enum 정의 (`claude`, `slack`, `gmail`, `github`, `discord`, `jira`, `linear`, `teams`)
- [x] `ConnectionAuthType` enum 정의 (`apiKey`, `oauth`, `signature`, `system`)
- [x] `ConnectionStatus` enum 정의 (`pending`, `active`, `needsAction`, `revoked`, `error`)
- [x] `ConnectionCapability` enum 정의 (`webhookReceive`, `testMessage`, `refreshToken`, `rotateKey`)
- [x] backend enum 대문자 값과 frontend camelCase 값 변환 로직 정의
- [x] `ConnectionEntity` 정의
- [x] `ConnectionModel` 정의
- [x] `ConnectionCreateRequest` 정의
- [x] `ConnectionCreateResponse` 정의
- [x] `ConnectionOAuthUrlResponse` 정의
- [x] `ConnectionTestResponse` 정의
- [x] `ConnectionActionResult` 정의 (`ConnectionRefreshResponse`, `ConnectionRotateKeyResponse`)
- [ ] `ConnectionEventModel` 정의 (Phase 1에서 필요 시 구현)

### API 응답 계약

- [x] 기존 `ApiResponse<T>` 파싱 구조 재사용
- [x] 페이지네이션 응답 구조 재사용
- [x] connection 목록/상세 응답에 원문 API Key가 없음을 타입으로 반영
- [x] API Key 생성 응답에만 nullable이 아닌 `api_key` 필드 허용
- [x] API Key rotate 응답에만 nullable이 아닌 `api_key` 필드 허용
- [x] 원문 API Key를 로컬 DB, secure storage, shared preferences에 저장하지 않도록 정책 명시

### Error 처리

- [x] `RATE_LIMIT_EXCEEDED` 표시 문구 정의
- [x] `CONNECTION_NOT_FOUND` 표시 문구 정의
- [x] `CONNECTION_PROVIDER_UNSUPPORTED` 표시 문구 정의
- [x] `CONNECTION_AUTH_TYPE_UNSUPPORTED` 표시 문구 정의
- [x] `CONNECTION_ALREADY_EXISTS` 표시 문구 정의
- [x] `CONNECTION_VERIFICATION_FAILED` 표시 문구 정의
- [x] `OAUTH_STATE_INVALID` 표시 문구 정의
- [x] `OAUTH_CALLBACK_FAILED` 표시 문구 정의
- [x] `429` 응답의 `Retry-After` header를 사용자 메시지에 반영할지 결정

---

## Phase 1. Connections 데이터 레이어

> 목표: `/api/v1/connections/**`와 OAuth API를 호출하는 데이터 계층 구현

### Feature 구조

- [ ] `lib/features/connections/data/datasource/` 생성
- [ ] `lib/features/connections/data/model/` 생성
- [ ] `lib/features/connections/data/repository/` 생성
- [ ] `lib/features/connections/domain/entity/` 생성
- [ ] `lib/features/connections/domain/repository/` 생성
- [ ] `lib/features/connections/presentation/providers/` 생성
- [ ] `lib/features/connections/presentation/screens/` 생성
- [ ] `lib/features/connections/presentation/widgets/` 생성

### Remote DataSource

- [ ] `ConnectionRemoteDataSource` 구현
- [ ] `GET /api/v1/connections` 연동
- [ ] `GET /api/v1/connections/{id}` 연동
- [ ] `POST /api/v1/connections` 연동
- [ ] `DELETE /api/v1/connections/{id}` 연동
- [ ] `POST /api/v1/connections/{id}/test` 연동
- [ ] `POST /api/v1/connections/{id}/refresh` 연동
- [ ] `POST /api/v1/connections/{id}/rotate-key` 연동
- [ ] `POST /api/v1/connections/oauth-url` 연동
- [ ] `Dio` 공통 client와 `AuthInterceptor` 재사용
- [ ] connection API는 사용자 JWT access token 사용

### Repository

- [ ] `ConnectionRepository` interface 정의
- [ ] `ConnectionRepositoryImpl` 구현
- [ ] connection 목록 조회 메서드 구현
- [ ] connection 상세 조회 메서드 구현
- [ ] connection 생성 메서드 구현
- [ ] connection 삭제 메서드 구현
- [ ] connection test 메서드 구현
- [ ] connection refresh 메서드 구현
- [ ] API Key rotate 메서드 구현
- [ ] OAuth URL 요청 메서드 구현
- [ ] 원문 API Key는 repository 상태나 cache에 저장하지 않음
- [ ] API 실패 시 기존 network error 처리 패턴 준수

### OAuth 흐름

- [ ] OAuth 방식 provider 선택 시 backend authorize URL 요청
- [ ] authorize URL을 외부 브라우저 또는 platform browser로 열기
- [ ] callback 완료 후 connection 목록 refresh
- [ ] pending 상태 connection 표시
- [ ] OAuth 실패 상태 connection 표시
- [ ] `OAUTH_STATE_INVALID`와 `OAUTH_CALLBACK_FAILED` 에러 메시지 처리

---

## Phase 2. Connections 상태 관리

> 목표: 목록, 상세, 생성, rotate, revoke, OAuth 상태를 Riverpod으로 일관 관리

### Providers

- [ ] `connectionsProvider` 구현
- [ ] `connectionDetailProvider` 구현
- [ ] `connectionActionsProvider` 또는 action 상태 notifier 구현
- [ ] provider/status/auth type filter 상태 provider 구현
- [ ] one-time API key 표시 상태 provider 구현
- [ ] OAuth 진행 상태 provider 구현

### 목록 상태

- [ ] 최초 로딩 상태 구현
- [ ] pull-to-refresh 상태 구현
- [ ] empty state 처리
- [ ] error state 처리
- [ ] filter 변경 시 목록 재계산 또는 재조회
- [ ] create/delete/rotate/test/refresh 후 목록 refresh

### Action 상태

- [ ] connection 생성 중 loading 상태 처리
- [ ] connection 삭제 중 loading 상태 처리
- [ ] test 요청 중 loading 상태 처리
- [ ] refresh 요청 중 loading 상태 처리
- [ ] rotate-key 요청 중 loading 상태 처리
- [ ] action별 성공 메시지 처리
- [ ] action별 실패 메시지 처리
- [ ] rotate-key 성공 시 one-time API key dialog 표시

### One-time API Key 상태

- [ ] API Key 생성 성공 시 원문 key를 dialog 상태에만 보관
- [ ] API Key rotate 성공 시 원문 key를 dialog 상태에만 보관
- [ ] dialog 닫기 시 원문 key를 즉시 폐기
- [ ] 화면 재진입, refresh, 앱 재시작 후 원문 key 재표시 불가
- [ ] copy 성공/실패 상태 처리

---

## Phase 3. Connections UI 구현

> 목표: Settings에서 모든 외부 연동을 통합 관리하는 화면 제공

### Routing

- [ ] `SettingsScreen`에 `연동 관리` 진입점 추가
- [ ] `/settings/connections` route 추가
- [ ] connection 상세 route 추가 여부 결정 및 구현
- [ ] OAuth callback 이후 설정 화면 복귀 flow 점검

### ConnectionsScreen

- [ ] 전체 connection list 표시
- [ ] provider icon 표시
- [ ] display name 표시
- [ ] account label 표시
- [ ] status 표시
- [ ] auth type 표시
- [ ] last used 표시
- [ ] created at 표시
- [ ] All filter 구현
- [ ] Active filter 구현
- [ ] Needs Action filter 구현
- [ ] API Key filter 구현
- [ ] OAuth filter 구현
- [ ] pull-to-refresh 구현
- [ ] empty state 구현
- [ ] loading state 구현
- [ ] error state 구현

### CreateConnectionSheet

- [ ] provider 선택 UI 구현
- [ ] provider별 지원 auth type 표시
- [ ] API Key 방식이면 display name 입력
- [ ] OAuth 방식이면 display name 입력
- [ ] Claude API Key connection 생성 flow 구현
- [ ] Slack OAuth 시작 flow 구현
- [ ] Gmail OAuth 시작 flow 구현
- [ ] provider unsupported 상태 처리
- [ ] auth type unsupported 상태 처리

### OneTimeApiKeyDialog

- [ ] 원문 API Key 1회 표시
- [ ] copy 버튼 구현
- [ ] Claude Code shell snippet 표시
- [ ] `NOTIO_WEBHOOK_API_KEY=<api_key>` 형식 표시
- [ ] 닫기 확인 동작 구현
- [ ] 닫은 뒤 재조회 불가 문구 표시
- [ ] 원문 key가 로그에 출력되지 않도록 점검

### ConnectionDetailScreen

- [ ] connection 기본 정보 표시
- [ ] key preview 표시
- [ ] account label 표시
- [ ] capabilities 표시
- [ ] test 버튼 표시
- [ ] refresh 버튼 표시
- [ ] rotate key 버튼 표시
- [ ] disconnect 버튼 표시
- [ ] 최근 event 일부 표시
- [ ] rotate key 확인 dialog 구현
- [ ] disconnect 확인 dialog 구현

### Shared Widgets

- [ ] `ConnectionCard` 구현
- [ ] `ConnectionStatusBadge` 구현
- [ ] provider icon helper 구현
- [ ] auth type label helper 구현
- [ ] capability chip 구현
- [ ] `GlassCard` 재사용
- [ ] 색상은 `AppColors`만 사용
- [ ] 타이포그래피는 `AppTextStyles`만 사용
- [ ] 간격은 `AppSpacing`만 사용
- [ ] 텍스트 overflow와 모바일 레이아웃 점검

---

## Phase 4. Frontend 테스트 및 검증

> 목표: API Key 1회 표시와 connection 관리 flow를 회귀 테스트로 고정

### Unit 테스트

- [ ] `ConnectionModel.fromJson` 테스트
- [ ] `ConnectionModel.toEntity` 테스트
- [ ] enum serialization/deserialization 테스트
- [ ] `ConnectionRepositoryImpl` 성공 flow 테스트
- [ ] `ConnectionRepositoryImpl` error flow 테스트
- [ ] one-time API key 미저장 정책 테스트

### Provider 테스트

- [ ] connection list loading/success/error 상태 테스트
- [ ] filter 상태 테스트
- [ ] create 성공 후 one-time API key 표시 테스트
- [ ] rotate 성공 후 one-time API key 표시 테스트
- [ ] dialog 닫기 후 key 폐기 테스트
- [ ] delete 후 목록 refresh 테스트
- [ ] test/refresh action 상태 테스트

### Widget 테스트

- [ ] `ConnectionsScreen` list 표시 테스트
- [ ] empty state 표시 테스트
- [ ] error state 표시 테스트
- [ ] `CreateConnectionSheet` API Key flow 테스트
- [ ] `OneTimeApiKeyDialog` copy 버튼 표시 테스트
- [ ] `ConnectionDetailScreen` action 버튼 표시 테스트
- [ ] OAuth pending/active/error 상태 표시 테스트

### 수동 검증

- [ ] Settings에서 연동 관리 화면 진입 확인
- [ ] Claude connection 생성 확인
- [ ] API Key 복사 확인
- [ ] dialog 닫은 뒤 원문 key 재표시 불가 확인
- [ ] rotate-key 후 새 key 표시 및 목록 갱신 확인
- [ ] test 요청 후 상태 메시지 확인
- [ ] disconnect 후 목록 갱신 확인
- [ ] Slack/Gmail OAuth pending/error 표시 확인
- [ ] 모바일 화면에서 텍스트 겹침 없음 확인

### 품질 확인

- [ ] `flutter analyze` 통과
- [ ] `flutter test` 통과
- [ ] 필요 시 `flutter build apk --debug` 통과

