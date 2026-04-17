# Frontend Fix 개발 체크리스트

> 대상: notifications 목록/상세 API 분리 및 상세 조회 흐름 개선
> 범위: Flutter 3.x · Dart 3.6 · Notification 화면

---

## Phase 0. 화면 계약 정리

- [x] notifications 목록 화면은 목록 API 응답만 사용하도록 기준을 확정한다.
- [x] 상세 모달은 상세 API 응답만 사용하도록 기준을 확정한다.
- [x] 목록 카드에서 전체 `body` 대신 `bodyPreview`를 사용하는 방향으로 확정한다.
- [x] 상세 조회는 알림 탭 후 `GET /api/v1/notifications/{id}` 호출 완료 뒤 모달을 여는 흐름으로 확정한다.
- [x] 상세 조회 결과로 읽음 상태가 바뀌면 목록 상태와 unread badge를 동기화하는 것을 요구사항으로 고정한다.

## Phase 1. 데이터 모델 및 repository 계약 분리

- [x] 목록 전용 notification model/entity를 정의하거나 기존 타입을 목록 전용 구조로 축소한다.
- [x] 상세 전용 notification detail model/entity를 추가한다.
- [x] `fetchNotifications()`가 목록 응답 타입을 반환하도록 수정한다.
- [x] `getNotificationDetail(int id)` repository 계약을 추가한다.
- [x] remote datasource에 상세 조회 API 호출 메서드를 추가한다.
- [x] 목록/상세 JSON 파싱 로직을 분리한다.

## Phase 2. 목록 화면 렌더링 정리

- [x] `notifications_screen.dart`가 목록 응답 기준으로 렌더링되도록 수정한다.
- [x] 카드 본문 미리보기는 `bodyPreview`를 사용하도록 변경한다.
- [x] 목록 화면이 더 이상 상세 전용 필드(`externalUrl`, `metadata`, 전체 body`)에 의존하지 않도록 정리한다.
- [x] 기존 pagination, refresh, source filter 동작이 목록 전용 응답 구조에서도 유지되도록 정리한다.

## Phase 3. 상세 조회 흐름 변경

- [x] 알림 카드 탭 시 `getNotificationDetail(id)`를 호출하도록 변경한다.
- [x] 상세 조회 중 로딩 상태를 사용자에게 표시한다.
- [x] 상세 조회 성공 후 `NotificationDetailModal`을 열도록 변경한다.
- [x] 상세 조회 실패 시 에러 피드백을 제공하고 모달은 열지 않도록 정리한다.
- [x] 상세 조회 응답의 `isRead` 값이 true이면 목록 상태의 해당 알림을 읽음으로 갱신한다.
- [x] 상세 조회 전 unread였던 항목이면 `unreadCountProvider`를 invalidate 하도록 정리한다.

## Phase 4. 상세 모달 책임 정리

- [x] `NotificationDetailModal`이 상세 전용 entity/model만 받도록 정리한다.
- [x] 모달 내부에서 읽음 처리 API나 상태 변경 side effect를 수행하지 않도록 유지한다.
- [x] 모달은 상세 응답의 전체 `body`, `externalUrl`, `metadata`를 렌더링하도록 정리한다.
- [x] 공유/복사/외부 링크 액션이 상세 응답 구조와 일치하는지 확인한다.

## Phase 5. 상태 동기화 및 캐시 정리

- [x] 상세 조회 후 목록의 unread indicator가 즉시 사라지도록 상태 갱신 로직을 추가한다.
- [x] 상세 조회 후 목록 title style이 read 상태로 즉시 반영되도록 확인한다.
- [x] 상단 unread badge가 상세 조회 후 감소하도록 확인한다.
- [x] pull-to-refresh 이후 서버의 읽음 상태와 목록 상태가 일치하는지 확인한다.
- [x] 이번 범위에서 상세 데이터 로컬 캐시를 유지할지 제외할지 구현 기준을 명확히 한다.

## Phase 6. 테스트 및 검증

- [ ] 목록 API 파싱 테스트를 목록 DTO 기준으로 수정한다.
- [ ] 상세 API 파싱 테스트를 추가한다.
- [ ] 알림 탭 시 상세 API가 1회 호출되는 notifier/provider 테스트를 추가한다.
- [ ] 상세 조회 성공 후 목록 항목 `isRead` 갱신과 unread badge refresh를 검증하는 테스트를 추가한다.
- [ ] 상세 조회 실패 시 모달이 열리지 않고 에러 처리되는 테스트를 추가한다.
- [ ] 상세 모달이 상세 응답 데이터를 렌더링하는 widget test를 추가한다.
- [ ] `flutter analyze`를 실행한다.
- [ ] `flutter test`를 실행한다.
