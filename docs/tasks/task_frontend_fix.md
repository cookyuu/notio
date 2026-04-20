# Frontend Fix 개발 체크리스트

> 대상: Developer Menu 제거 및 관련 프론트 정리
> 범위: Flutter 3.x · Dart 3.6 · Settings / Router / Notifications

---

## Phase 0. 범위 확정

- [x] `Developer Menu`를 사용자 노출 기능이 아닌 임시 QA 화면으로 확정한다.
- [x] 이번 작업 범위를 `완전 제거`로 고정한다.
- [x] 제거 범위에 Settings 진입 버튼, 전용 화면, 전용 라우트, 전용 dead code 정리를 포함한다.
- [x] 유지 범위에 `LocalNotificationService`, `NotificationPayload`, 일반 알림 화면 기능을 포함한다.
- [x] 이번 작업에 새로운 제품 기능 추가는 포함하지 않음을 명시한다.

## Phase 1. Settings 화면 정리

- [x] Settings 화면에서 `개발자` 섹션 헤더를 제거한다.
- [x] Settings 화면에서 `개발자 메뉴` ListTile을 제거한다.
- [x] 화면 내 관련 divider/spacing이 어색하지 않도록 섹션 구성을 정리한다.
- [x] Settings 화면이 제거 후에도 `외관`, `알림`, `연동`, `계정`, `정보` 흐름을 자연스럽게 유지하는지 확인한다.

## Phase 2. 라우팅 및 화면 제거

- [ ] `Routes.developer` 경로 상수를 제거한다.
- [ ] `app_router.dart`에서 `DeveloperMenuScreen` import를 제거한다.
- [ ] `app_router.dart`에서 `/developer` 라우트 등록을 제거한다.
- [ ] `developer_menu_screen.dart` 파일을 삭제한다.
- [ ] 제거 후 라우팅 구조가 인증 화면, 메인 탭, 연동 상세 화면에 영향을 주지 않는지 확인한다.

## Phase 3. 알림 관련 dead code 정리

- [ ] `NotificationsNotifier`의 `addTestNotification()` 사용 여부를 점검한다.
- [ ] 실제 참조가 없으면 `addTestNotification()`을 제거한다.
- [ ] `clearCache()`가 Developer Menu 전용 책임이라면 제거하거나 일반 기능으로 남길 근거를 명확히 한다.
- [ ] Developer Menu 전용 주석, 설명, 임시 문자열을 함께 정리한다.
- [ ] 테스트 전용 payload 문자열(`test_notification`) 의존이 남지 않도록 확인한다.

## Phase 4. 문서 및 표시 문구 정리

- [ ] 프론트 문서에서 `개발자 메뉴 구현 완료`처럼 현재 제품 방향과 맞지 않는 문구를 찾는다.
- [ ] 이번 작업 범위 안에서 수정 가능한 문서만 최소 정리한다.
- [ ] Settings 화면 내 사용자 노출 문구가 제거 후 더 간결해졌는지 확인한다.
- [ ] 버전/Phase 표기가 이번 변경과 충돌하지 않는지 확인한다.

## Phase 5. 테스트 및 검증

- [ ] `flutter analyze`를 실행한다.
- [ ] 관련 widget/router 테스트가 있으면 제거된 UI와 라우트 기준으로 갱신한다.
- [ ] Settings 화면에서 `개발자 메뉴` 텍스트가 더 이상 노출되지 않는지 확인한다.
- [ ] `/developer` 직접 진입 경로가 제거되었는지 확인한다.
- [ ] Notifications, Settings, Connections 기본 이동 흐름이 유지되는지 확인한다.
- [ ] 필요 시 수동 검증 메모를 남긴다.
