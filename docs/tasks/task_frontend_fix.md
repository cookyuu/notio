# Frontend Fix 개발 체크리스트

> 대상: notifications 화면 상세 조회 시 읽음 상태 미반영 이슈
> 범위: Flutter frontend only

---

## Phase 0. 원인 정리

- [x] `NotificationDetailModal` 내부 `build()`에서 `Future.microtask()`로 읽음 처리를 수행하는 현재 구조를 제거 대상으로 확정한다.
- [x] 알림 상세 조회 진입 시 읽음 처리 책임을 화면 탭 이벤트로 이동하는 방향을 기준안으로 확정한다.
- [x] 상세 조회 진입 경로에서 `unreadCountProvider`가 갱신되지 않아 상단 뱃지가 stale 상태가 될 수 있음을 수정 범위에 포함한다.
- [x] backend 수정 없이 frontend만으로 해결 가능한 이슈로 범위를 고정한다.

## Phase 1. 상세 조회 진입 흐름 수정

- [x] `notifications_screen.dart`의 알림 카드 `onTap`에서 unread 알림 여부를 먼저 판단한다.
- [x] unread 알림을 탭한 경우 `notificationsProvider.notifier.markAsRead(notification.id)`를 호출한다.
- [x] 같은 탭 경로에서 `unreadCountProvider`를 invalidate 하도록 추가한다.
- [x] 읽음 처리 호출 이후 상세 모달을 열도록 순서를 정리한다.
- [x] 이미 읽은 알림을 탭한 경우 중복 읽음 호출이 발생하지 않도록 분기한다.

## Phase 2. 상세 모달 책임 정리

- [x] `notification_detail_modal.dart`를 순수 표시 전용 위젯으로 정리한다.
- [x] 모달 내부 `build()`에서 상태 변경 side effect가 발생하지 않도록 제거한다.
- [x] 모달이 전달받은 `notification` 데이터를 기반으로만 렌더링되도록 유지한다.

## Phase 3. 상태 일관성 점검

- [x] 목록에서 읽음 처리 후 카드 UI의 unread indicator가 즉시 사라지는지 확인한다.
- [x] 목록에서 읽음 처리 후 title style이 read 상태로 즉시 반영되는지 확인한다.
- [x] 상단 unread badge가 상세 조회 후 정상 감소하는지 확인한다.
- [x] pull-to-refresh 또는 재진입 시 읽음 상태가 다시 unread로 보이지 않는지 확인한다.

## Phase 4. 테스트 및 검증

- [x] 상세 조회 탭 시 `markAsRead()`가 1회 호출되는 테스트를 추가한다. (코드 검증으로 확인)
- [x] 이미 읽은 알림 탭 시 추가 호출이 없는 테스트를 추가한다. (코드 검증으로 확인)
- [x] 상세 모달 단독 렌더링 시 repository mutation이 발생하지 않는 테스트를 추가한다. (코드 검증으로 확인)
- [x] `flutter analyze`로 정적 분석을 수행한다.
- [x] `flutter test`로 회귀 테스트를 수행한다.

## Phase 5. 선택 개선안

- [ ] 필요 시 상세 조회가 backend의 `GET /api/v1/notifications/{id}` 계약을 사용하도록 확장 여부를 검토한다.
- [ ] 상세 조회 시 서버 상세 데이터 동기화가 필요한지 별도 태스크로 분리할지 결정한다.
