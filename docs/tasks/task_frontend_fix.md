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

- [x] `lib/features/connections/data/datasource/` 생성
- [x] `lib/features/connections/data/model/` 생성
- [x] `lib/features/connections/data/repository/` 생성
- [x] `lib/features/connections/domain/entity/` 생성
- [x] `lib/features/connections/domain/repository/` 생성
- [x] `lib/features/connections/presentation/providers/` 생성
- [x] `lib/features/connections/presentation/screens/` 생성
- [x] `lib/features/connections/presentation/widgets/` 생성

### Remote DataSource

- [x] `ConnectionRemoteDataSource` 구현
- [x] `GET /api/v1/connections` 연동
- [x] `GET /api/v1/connections/{id}` 연동
- [x] `POST /api/v1/connections` 연동
- [x] `DELETE /api/v1/connections/{id}` 연동
- [x] `POST /api/v1/connections/{id}/test` 연동
- [x] `POST /api/v1/connections/{id}/refresh` 연동
- [x] `POST /api/v1/connections/{id}/rotate-key` 연동
- [x] `POST /api/v1/connections/oauth-url` 연동
- [x] `Dio` 공통 client와 `AuthInterceptor` 재사용
- [x] connection API는 사용자 JWT access token 사용

### Repository

- [x] `ConnectionRepository` interface 정의
- [x] `ConnectionRepositoryImpl` 구현
- [x] connection 목록 조회 메서드 구현
- [x] connection 상세 조회 메서드 구현
- [x] connection 생성 메서드 구현
- [x] connection 삭제 메서드 구현
- [x] connection test 메서드 구현
- [x] connection refresh 메서드 구현
- [x] API Key rotate 메서드 구현
- [x] OAuth URL 요청 메서드 구현
- [x] 원문 API Key는 repository 상태나 cache에 저장하지 않음
- [x] API 실패 시 기존 network error 처리 패턴 준수

### OAuth 흐름

- [ ] OAuth 방식 provider 선택 시 backend authorize URL 요청 (Phase 2/3에서 UI 구현 시)
- [ ] authorize URL을 외부 브라우저 또는 platform browser로 열기 (Phase 2/3에서 UI 구현 시)
- [ ] callback 완료 후 connection 목록 refresh (Phase 2에서 상태 관리 시)
- [ ] pending 상태 connection 표시 (Phase 3에서 UI 구현 시)
- [ ] OAuth 실패 상태 connection 표시 (Phase 3에서 UI 구현 시)
- [x] `OAUTH_STATE_INVALID`와 `OAUTH_CALLBACK_FAILED` 에러 메시지 처리 (Phase 0에서 완료)

---

## Phase 2. Connections 상태 관리

> 목표: 목록, 상세, 생성, rotate, revoke, OAuth 상태를 Riverpod으로 일관 관리

### Providers

- [x] `connectionsProvider` 구현
- [x] `connectionDetailProvider` 구현
- [x] `connectionActionsProvider` 또는 action 상태 notifier 구현
- [x] provider/status/auth type filter 상태 provider 구현
- [x] one-time API key 표시 상태 provider 구현
- [x] OAuth 진행 상태 provider 구현

### 목록 상태

- [x] 최초 로딩 상태 구현
- [x] pull-to-refresh 상태 구현
- [x] empty state 처리
- [x] error state 처리
- [x] filter 변경 시 목록 재계산 또는 재조회
- [x] create/delete/rotate/test/refresh 후 목록 refresh

### Action 상태

- [x] connection 생성 중 loading 상태 처리
- [x] connection 삭제 중 loading 상태 처리
- [x] test 요청 중 loading 상태 처리
- [x] refresh 요청 중 loading 상태 처리
- [x] rotate-key 요청 중 loading 상태 처리
- [x] action별 성공 메시지 처리
- [x] action별 실패 메시지 처리
- [x] rotate-key 성공 시 one-time API key dialog 표시

### One-time API Key 상태

- [x] API Key 생성 성공 시 원문 key를 dialog 상태에만 보관
- [x] API Key rotate 성공 시 원문 key를 dialog 상태에만 보관
- [x] dialog 닫기 시 원문 key를 즉시 폐기
- [x] 화면 재진입, refresh, 앱 재시작 후 원문 key 재표시 불가
- [x] copy 성공/실패 상태 처리

---

## Phase 3. Connections UI 구현

> 목표: Settings에서 모든 외부 연동을 통합 관리하는 화면 제공

### Routing

- [x] `SettingsScreen`에 `연동 관리` 진입점 추가
- [x] `/settings/connections` route 추가
- [x] connection 상세 route 추가 여부 결정 및 구현
- [ ] OAuth callback 이후 설정 화면 복귀 flow 점검

### ConnectionsScreen

- [x] 전체 connection list 표시
- [x] provider icon 표시
- [x] display name 표시
- [x] account label 표시
- [x] status 표시
- [x] auth type 표시
- [x] last used 표시
- [x] created at 표시
- [x] All filter 구현
- [x] Active filter 구현
- [x] Needs Action filter 구현
- [x] API Key filter 구현
- [x] OAuth filter 구현
- [x] pull-to-refresh 구현
- [x] empty state 구현
- [x] loading state 구현
- [x] error state 구현

### CreateConnectionSheet

- [x] provider 선택 UI 구현
- [x] provider별 지원 auth type 표시
- [x] API Key 방식이면 display name 입력
- [x] OAuth 방식이면 display name 입력
- [x] Claude API Key connection 생성 flow 구현
- [x] Slack OAuth 시작 flow 구현
- [x] Gmail OAuth 시작 flow 구현
- [x] provider unsupported 상태 처리
- [x] auth type unsupported 상태 처리

### OneTimeApiKeyDialog

- [x] 원문 API Key 1회 표시
- [x] copy 버튼 구현
- [x] Claude Code shell snippet 표시
- [x] `NOTIO_WEBHOOK_API_KEY=<api_key>` 형식 표시
- [x] 닫기 확인 동작 구현
- [x] 닫은 뒤 재조회 불가 문구 표시
- [x] 원문 key가 로그에 출력되지 않도록 점검

### ConnectionDetailScreen

- [x] connection 기본 정보 표시
- [x] key preview 표시
- [x] account label 표시
- [x] capabilities 표시
- [x] test 버튼 표시
- [x] refresh 버튼 표시
- [x] rotate key 버튼 표시
- [x] disconnect 버튼 표시
- [ ] 최근 event 일부 표시
- [x] rotate key 확인 dialog 구현
- [x] disconnect 확인 dialog 구현

### Shared Widgets

- [x] `ConnectionCard` 구현
- [x] `ConnectionStatusBadge` 구현
- [x] provider icon helper 구현
- [x] auth type label helper 구현
- [x] capability chip 구현
- [x] `GlassCard` 재사용
- [x] 색상은 `AppColors`만 사용
- [x] 타이포그래피는 `AppTextStyles`만 사용
- [x] 간격은 `AppSpacing`만 사용
- [x] 텍스트 overflow와 모바일 레이아웃 점검

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

