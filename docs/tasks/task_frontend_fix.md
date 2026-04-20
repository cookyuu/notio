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

- [x] `Routes.developer` 경로 상수를 제거한다.
- [x] `app_router.dart`에서 `DeveloperMenuScreen` import를 제거한다.
- [x] `app_router.dart`에서 `/developer` 라우트 등록을 제거한다.
- [x] `developer_menu_screen.dart` 파일을 삭제한다.
- [x] 제거 후 라우팅 구조가 인증 화면, 메인 탭, 연동 상세 화면에 영향을 주지 않는지 확인한다.

### Phase 2 확인 메모

- `frontend/lib/core/router/routes.dart`에서 `Routes.developer` 상수를 제거했고, `frontend/lib/core/router/app_router.dart`에서 `DeveloperMenuScreen` import 및 `/developer` 라우트 등록을 함께 삭제했다.
- 연결 상세 라우트는 계속 `Routes.connections` 하위의 `:id` 중첩 구조를 유지하고, 인증 화면과 메인 탭 라우트도 변경하지 않았다.
- `frontend/lib/features/settings/presentation/screens/developer_menu_screen.dart`는 더 이상 참조되지 않아 삭제 대상으로 확정했다.
- 이번 Phase 2 범위의 결론은 `developer 전용 진입 경로 제거 완료`이며, 인증 화면, 메인 탭, 연동 상세 화면과 직접 연결된 라우팅 구조에는 영향이 없다.

## Phase 3. 알림 관련 dead code 정리

- [x] `NotificationsNotifier`의 `addTestNotification()` 사용 여부를 점검한다.
- [x] 실제 참조가 없으면 `addTestNotification()`을 제거한다.
- [x] `clearCache()`가 Developer Menu 전용 책임이라면 제거하거나 일반 기능으로 남길 근거를 명확히 한다.
- [x] Developer Menu 전용 주석, 설명, 임시 문자열을 함께 정리한다.
- [x] 테스트 전용 payload 문자열(`test_notification`) 의존이 남지 않도록 확인한다.

### Phase 3 확인 메모

- `NotificationsNotifier`의 `addTestNotification()`은 코드베이스 검색 기준 실제 참조가 없었고, 제거된 Developer Menu 전용 테스트 주입 API였기 때문에 삭제했다.
- `clearCache()`는 삭제하지 않고 일반 캐시 무효화 책임으로 유지했다. 이 메서드는 `NotificationRepository.clearCache()`와 연결되어 로컬 알림 캐시를 초기화하고 목록 상태/페이지네이션을 리셋하는 공용 동작으로 해석할 수 있다.
- `NotificationsNotifier` 내부의 `for developer menu` 주석을 제거해 현재 제품 책임과 맞지 않는 설명을 정리했다.
- `test_notification` 문자열은 삭제된 `developer_menu_screen.dart`에만 존재했고, 현재 `frontend/lib`, `frontend/test` 범위 검색 기준 잔여 의존은 없다.
- 이번 Phase 3 결론은 `developer 전용 알림 dead code 정리 완료`이며, 일반 알림 목록 조회/상세 조회/캐시 구조는 유지된다.

## Phase 4. 문서 및 표시 문구 정리

- [x] 프론트 문서에서 `개발자 메뉴 구현 완료`처럼 현재 제품 방향과 맞지 않는 문구를 찾는다.
- [x] 이번 작업 범위 안에서 수정 가능한 문서만 최소 정리한다.
- [x] Settings 화면 내 사용자 노출 문구가 제거 후 더 간결해졌는지 확인한다.
- [x] 버전/Phase 표기가 이번 변경과 충돌하지 않는지 확인한다.

### Phase 4 확인 메모

- 프론트 문서 검색 결과 `docs/tasks/task_frontend.md`의 Phase 4A 기록에 `개발자 메뉴 화면 구현`, `설정 화면에 개발자 메뉴 버튼 추가`, `개발자 메뉴 구현 완료` 같은 현재 방향과 맞지 않는 표현이 남아 있었다.
- 이번 작업 범위에서는 과거 문서를 전면 개편하지 않고, 해당 문서의 상태 문구와 참고사항만 최소 수정해 현재 기준으로는 Developer Menu가 제거되었음을 드러내도록 정리했다.
- Settings 화면의 사용자 노출 버전 정보는 `v1.0.0 (Phase 4A)`에서 `v1.0.0`으로 단순화했다. 내부 개발 Phase 표기를 사용자에게 그대로 노출할 필요가 없고, 이번 제거 작업과도 직접 관련이 없기 때문이다.
- 버전/Phase 충돌 여부는 사용자 노출 문구 기준으로 해소했다. 제품 화면에서는 더 이상 Developer Menu나 `Phase 4A` 같은 내부 구현 단계 표현이 보이지 않는다.

## Phase 5. 테스트 및 검증

- [ ] `flutter analyze`를 실행한다.
- [x] 관련 widget/router 테스트가 있으면 제거된 UI와 라우트 기준으로 갱신한다.
- [x] Settings 화면에서 `개발자 메뉴` 텍스트가 더 이상 노출되지 않는지 확인한다.
- [x] `/developer` 직접 진입 경로가 제거되었는지 확인한다.
- [x] Notifications, Settings, Connections 기본 이동 흐름이 유지되는지 확인한다.
- [x] 필요 시 수동 검증 메모를 남긴다.

### Phase 5 확인 메모

- `flutter analyze`는 이번 세션에서 실행을 시도했지만, 현재 환경이 Windows Flutter SDK를 직접 실행할 수 없어 완료하지 못했다. `cmd.exe /c flutter.bat analyze`는 WSL 실행 환경에서 `Exec format error`가 발생했고, `flutter.bat analyze`도 배치 파일을 POSIX 셸이 해석하면서 실패했다.
- 관련 widget/router 테스트는 제거된 Developer Menu 전용 화면이나 `/developer` 라우트를 직접 다루는 케이스가 별도로 없어서 추가 수정이 필요하지 않았다. 기존 `app_router_auth_guard_test.dart`, `notifications_screen_test.dart`, `connections_screen_test.dart`가 핵심 인증/알림/연동 화면 구조를 계속 커버한다.
- `개발자 메뉴` 텍스트는 현재 `frontend/lib`, `frontend/test` 검색 기준 제품 코드에 남아 있지 않고, 체크리스트 문서 설명에만 존재한다.
- `/developer` 직접 진입 경로도 현재 제품 코드 기준 제거 완료 상태다. `Routes.developer`, `DeveloperMenuScreen`, `/developer` 라우트 등록은 모두 삭제되었다.
- 기본 이동 흐름은 코드 구조상 유지된다. `NotificationsScreen`, `SettingsScreen`, `ConnectionsScreen` 및 연결 상세 라우트는 그대로 남아 있고, 이번 수정은 developer 전용 진입점 제거에 한정된다.
- 수동 검증 메모:
  - 가능하면 Windows 네이티브 셸에서 `flutter analyze`와 `flutter test`를 한 번 더 실행한다.
  - 앱 실행 후 Settings 화면에 developer 관련 항목이 없는지 확인한다.
  - 알림 탭, 설정 탭, 연동 관리 화면, 연동 상세 화면 진입이 기존과 동일한지 확인한다.
