# Task: Frontend 개발 체크리스트 (GoException 라우팅 버그 수정)

> **대상 버전**: v2.1
> **작성일**: 2026-05-15
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## Step 1: delivery_bubble.dart — onTap nullable 수정

**파일**: `lib/features/delivery_feed/presentation/widgets/delivery_bubble.dart`

- [x] `onTap` 필드 타입 변경
  - [x] `final VoidCallback onTap` → `final VoidCallback? onTap`
  - [x] `GestureDetector.onTap`은 이미 `GestureTapCallback?`이므로 추가 변경 불필요 확인
- [x] `delivery_feed_screen.dart` 144번째 줄 로딩 중 `null` 전달 — 타입 불일치 해소 확인

---

## Step 2: routes.dart — 경로 상수 및 헬퍼 추가

**파일**: `lib/core/router/routes.dart`

- [x] `/notifications/:id` 경로 상수 추가
  - [x] `static const String notificationDetail = '/notifications/:id'`
- [x] `notificationDetailPath` 헬퍼 메서드 추가
  - [x] `static String notificationDetailPath(int id) => '/notifications/$id'`

---

## Step 3: notification_detail_screen.dart — 전체 화면 상세 (신규)

**파일**: `lib/features/notification/presentation/screens/notification_detail_screen.dart`

- [x] `NotificationDetailScreen` 위젯 생성
  - [x] `ConsumerStatefulWidget` 상속
  - [x] `notificationId` (`int`) 파라미터 선언
- [x] `initState`에서 `fetchNotificationDetail` 호출
- [x] 로딩 중 `CircularProgressIndicator` 표시
- [x] 데이터 로드 완료 후 `NotificationDetailModal` 콘텐츠를 `SingleChildScrollView`로 감싸 재사용
- [x] `AppBar` 구성
  - [x] `context.canPop()` 시 `context.pop()`
  - [x] `context.canPop()` false 시 `context.go(Routes.notifications)` 폴백

---

## Step 4: app_router.dart — 4가지 수정

**파일**: `lib/core/router/app_router.dart`

### 4-A. import 추가

- [x] `notification_detail_screen.dart` import 추가

### 4-B. ShellRoute 내 `/notifications` 중첩 라우트 추가

- [x] `Routes.notifications` GoRoute에 `routes` 목록 추가
  - [x] 중첩 `GoRoute(path: ':id')` 추가 → `/notifications/:id`
  - [x] `pageBuilder`에서 `state.pathParameters['id']` 파싱 (`int.parse`)
  - [x] `NoTransitionPage(child: NotificationDetailScreen(notificationId: id))` 반환

### 4-C. GoRouter errorBuilder 추가

- [x] `GoRouter`에 `errorBuilder` 추가
  - [x] `AppBar` — `BackButton`(`onPressed`: `context.go(Routes.notifications)`) + `title: Text('오류')`
  - [x] `body` — `Icon(Icons.error_outline)` + `Text('페이지를 찾을 수 없습니다')` + `FilledButton('홈으로 이동')`
  - [x] `FilledButton.onPressed`: `context.go(Routes.notifications)`
  - [x] `AppColors.error`, `AppTextStyles.headlineSmall`, `AppSpacing` 상수 사용

### 4-D. _MainScaffold — 상세 화면에서 BottomNav 숨김

- [x] `_MainScaffold.build()`에서 현재 경로 확인
  - [x] `GoRouterState.of(context).uri.path` 로 `location` 추출
  - [x] `RegExp(r'^/notifications/\d+$').hasMatch(location)` 로 `isDetailRoute` 판별
  - [x] `isDetailRoute`가 true이면 `bottomNavigationBar: null`

### 4-E. _BottomNavBar.getCurrentIndex() — prefix 매칭으로 변경

- [x] `switch-case` → `if-else` prefix 매칭으로 교체
  - [x] `location.startsWith(Routes.notifications)` → 0 반환
  - [x] `location.startsWith(Routes.chat)` → 1 반환
  - [x] `location == Routes.analytics` → 2 반환
  - [x] `location == Routes.settings` → 3 반환
  - [x] 기본값 0 반환

---

## 최종 검증

- [ ] `flutter analyze` — 타입 에러 0개
- [ ] Deliveries 화면에서 알림 항목 탭 → `showModalBottomSheet` 정상 표시
- [ ] Notifications 화면에서 알림 카드 탭 → `showModalBottomSheet` 정상 표시 (기존 동작 유지)
- [ ] 딥링크 `/notifications/316` 직접 접근 → `NotificationDetailScreen` 렌더
- [ ] 잘못된 경로 접근 → `errorBuilder` 화면 표시 (날 것의 GoException 대신)
- [ ] `/notifications/316` 화면에서 뒤로가기 → `/notifications` 복귀
- [ ] `/notifications/316` 화면에서 바텀네비게이션 미표시 확인
- [ ] 바텀네비게이션 탭 전환 정상 동작 확인
