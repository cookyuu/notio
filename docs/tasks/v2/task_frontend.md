# Task: Frontend 개발 체크리스트 (Deliveries 메시지 탭 버그 수정)

> **대상 버전**: v2.2
> **작성일**: 2026-05-14
> **연관 Plan**: `docs/plans/v2/plan_deliveries_fix.md`

---

## 배경

Deliveries 탭에서 메시지를 탭하면 아무 반응이 없음.
원인: `onTap`이 `context.push('/notifications/:id')`를 호출하지만
GoRouter에 해당 경로가 등록되어 있지 않아 라우팅 실패.

---

## Phase 1: DeliveryFeedScreen onTap 수정

**파일**: `frontend/lib/features/delivery_feed/presentation/screens/delivery_feed_screen.dart`

### 1-1. Import 추가

- [x] `package:notio_app/features/notification/presentation/providers/notifications_notifier.dart` import 추가
- [x] `package:notio_app/features/notification/presentation/widgets/notification_detail_modal.dart` import 추가

### 1-2. 로딩 상태 추가

- [x] `_DeliveryFeedScreenState`에 `int? _loadingItemId` 필드 추가

### 1-3. `_openDetail` 메서드 구현

- [x] `Future<void> _openDetail(int notificationId)` 메서드 추가
  - [x] `_loadingItemId != null` 시 조기 리턴 (중복 탭 방지)
  - [x] `setState(() => _loadingItemId = notificationId)` 호출
  - [x] `ref.read(notificationsProvider.notifier).fetchNotificationDetail(notificationId)` 호출
  - [x] `mounted` 체크 후 `showModalBottomSheet` 호출
    - [x] `isScrollControlled: true`, `backgroundColor: Colors.transparent` 설정
    - [x] `NotificationDetailModal(notification: detail)` 표시
  - [x] `catch`: 스낵바로 오류 메시지 표시
  - [x] `finally`: `setState(() => _loadingItemId = null)` 호출

### 1-4. `onTap` 교체

- [x] 기존 `context.push('/notifications/${item.notificationId}')` 제거
- [x] `onTap: _loadingItemId == item.notificationId ? null : () => _openDetail(item.notificationId)` 로 교체

---

## Phase 2: 검증

- [ ] `flutter analyze` — 경고 0개 확인
- [ ] Deliveries 탭 메시지 탭 → `NotificationDetailModal` 바텀 시트 정상 표시
- [ ] 로딩 중 동일 항목 중복 탭 → 모달이 두 번 열리지 않는지 확인
- [ ] 네트워크 오류 상황 → 스낵바 에러 메시지 표시 확인
- [ ] 모달 닫기(드래그 다운 / 닫기 버튼) 정상 동작 확인
