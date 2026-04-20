# Backend Fix 개발 체크리스트

> 대상: Developer Menu 제거에 따른 백엔드 영향도 점검 및 문서 정리
> 범위: Spring Boot 4.x · Java 25 · API 계약 / 연동 영향 확인

---

## Phase 0. 영향 범위 확인

- [x] `Developer Menu`가 백엔드 API의 공식 기능이 아닌 프론트 내부 QA 화면이었음을 기준으로 확정한다.
- [x] 이번 작업에서 신규 백엔드 API 개발은 필요하지 않음을 명시한다.
- [x] 이번 작업에서 기존 알림 API 계약 변경이 없는지 확인한다.
- [x] Settings/Developer Menu 제거가 인증, 알림, 연동 API와 직접 결합돼 있지 않은지 점검한다.

### Phase 0 확인 메모

- `Developer Menu` 흔적은 현재 프론트 라우트/화면(`frontend/lib/features/settings/presentation/screens/developer_menu_screen.dart`, `frontend/lib/core/router/app_router.dart`)에 한정되어 있으며, 백엔드 컨트롤러/Swagger/OpenAPI에는 대응 기능이 없다.
- 알림 API는 기존 `NotificationController` 기준으로 `GET /api/v1/notifications`, `GET /api/v1/notifications/{id}`, 읽음 처리/삭제 계열만 제공하며, Developer Menu 제거로 계약 변경이 필요한 항목은 확인되지 않았다.
- 인증/연동 경로는 각각 `AuthController`, `ConnectionController`, `WebhookController`로 분리되어 있고 `/developer`, `debug`, `mock notification` 성격의 백엔드 엔드포인트는 확인되지 않았다.
- 따라서 이번 범위의 Phase 0 결론은 `frontend-only cleanup`이며, 신규 백엔드 API 개발이나 기존 API 계약 수정은 필요하지 않다.

## Phase 1. 알림 계약 영향 점검

- [x] 로컬 알림 테스트 화면 제거가 `GET /api/v1/notifications` 및 상세 조회 계약에 영향을 주지 않는지 확인한다.
- [x] 알림 클릭 후 앱 라우팅 로직이 백엔드 payload 계약에 의존하지 않는지 확인한다.
- [x] 백엔드에 `developer`, `debug`, `mock notification` 성격의 엔드포인트가 남아 있는지 점검한다.
- [x] 남아 있다면 실제 제품 요구사항인지 삭제 후보인지 분류한다.

### Phase 1 확인 메모

- 백엔드 알림 계약은 계속 `NotificationController`의 `GET /api/v1/notifications`, `GET /api/v1/notifications/{id}`, 읽음 처리/삭제 계열에 한정되며, Developer Menu 제거와 연결된 전용 필드나 응답 포맷은 확인되지 않았다.
- 목록 계약은 `NotificationSummaryResponse` 기준 `id`, `source`, `title`, `priority`, `is_read`, `created_at`, `body_preview`를 반환하고, 상세 계약은 `NotificationDetailResponse` 기준 `connection_id`, `body`, `external_id`, `external_url`, `metadata`를 포함한다. 로컬 알림 테스트 화면 제거로 이 계약을 수정해야 할 근거는 없다.
- 프론트 알림 탭 처리 로직은 `frontend/lib/main.dart`에서 payload를 로깅만 하고 실제 라우팅은 항상 `/notifications`로 이동한다. 현재 앱 라우팅은 백엔드가 내려주는 payload 구조에 의존하지 않는다.
- `notification_remote_datasource.dart`도 백엔드 응답의 `data.content` 또는 `data` 리스트를 파싱할 뿐 Developer Menu 전용 파라미터나 mock payload를 요구하지 않는다.
- 백엔드 전체 검색 기준 `developer`, `/developer`, `debug`, `mock notification` 성격의 사용자용 엔드포인트는 확인되지 않았다. 검색 결과의 `debug`는 인증 필터 로그 수준, `mock`은 테스트 코드의 Mockito/MockMvc 문맥이다.
- 따라서 Phase 1 결론은 `백엔드 계약 영향 없음`이며, 분류가 필요한 잔여 developer/debug/mock notification 엔드포인트도 없다.

## Phase 2. 문서 및 태스크 정리

- [x] 백엔드 문서나 체크리스트에서 `Developer Menu`를 실제 백엔드 기능처럼 설명한 부분이 있는지 찾는다.
- [x] 이번 작업 범위에 필요한 최소 문서 수정만 반영한다.
- [x] 프론트 전용 임시 QA 흐름을 백엔드 책임처럼 오해할 수 있는 서술을 정리한다.

### Phase 2 확인 메모

- 백엔드 문서 범위(`AGENTS.md`, `docs/blueprint/notio_blueprint.md`, `backend/src/main/resources`, `backend/src/main/java`)를 재검색한 결과 `Developer Menu`를 실제 백엔드 기능으로 설명한 서술은 확인되지 않았다.
- `Developer Menu` 관련 명시적 언급은 현재 `docs/tasks/task_backend_fix.md`와 프론트 작업 문서/코드에만 남아 있으며, 백엔드 API 문서나 아키텍처 문서에는 연결된 기능 설명이 없다.
- 따라서 이번 Phase 2의 최소 문서 수정은 본 태스크 문서에 확인 결과를 남기고 체크리스트를 완료 처리하는 수준으로 제한한다.
- 프론트 전용 임시 QA 흐름에 대한 설명 정리와 잔여 문구 삭제는 `docs/tasks/task_frontend_fix.md` 범위에서 계속 관리한다.

## Phase 3. 검증 및 후속 메모

- [ ] 이번 변경으로 백엔드 코드 수정이 불필요하면 `no backend code change`로 명시한다.
- [ ] API 스펙, Swagger, 테스트 케이스 수정 필요 여부를 최종 확인한다.
- [ ] 후속 작업이 필요하면 별도 백로그로 분리한다.
- [ ] 프론트 제거 작업 이후 통합 검증 시 확인할 항목을 메모한다.
