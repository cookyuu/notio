# Task: Frontend 개발 체크리스트 (채널 삭제·수정)

> **대상 버전**: v2.1 (fix)
> **작성일**: 2026-05-13
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## Phase 1: 모델·엔티티에 `targetIdentifier` 추가

백엔드 `ChannelResponse`의 `target_identifier` 필드를 Flutter 모델/엔티티에 반영하여
수정 폼에서 pre-fill이 가능하도록 한다.

- [x] `lib/features/channels/domain/entity/notification_channel_entity.dart`
  - [x] `final String? targetIdentifier` 필드 추가
- [x] `lib/features/channels/data/model/notification_channel_model.dart`
  - [x] `@JsonKey(name: 'target_identifier') final String? targetIdentifier` 추가
  - [x] 생성자에 optional 파라미터 추가
  - [x] `toEntity()`에서 `targetIdentifier` 전달
- [x] `dart run build_runner build --delete-conflicting-outputs` 실행 후 `.g.dart` 재생성 확인

---

## Phase 2: 데이터 계층 — `updateChannel` 추가

DataSource → Repository 인터페이스 → Repository 구현체 순으로 추가한다.

- [x] `lib/features/channels/data/datasource/channel_remote_datasource.dart`
  - [x] `updateChannel(int id, Map<String, dynamic> data)` 메서드 추가
  - [x] `PUT /api/v1/channels/$id` 호출
  - [x] `response.data['success'] != true` 시 Exception throw
  - [x] `NotificationChannelModel.fromJson(response.data['data'])` 반환
- [x] `lib/features/channels/domain/repository/channel_repository.dart`
  - [x] `updateChannel({required int id, String? displayName, String? credentialPlaintext, String? targetIdentifier})` 추상 메서드 추가
- [x] `lib/features/channels/data/repository/channel_repository_impl.dart`
  - [x] `updateChannel` 구현
  - [x] null이 아닌 필드만 Map에 포함해서 전송 (`if (field != null) 'key': field` 패턴)

---

## Phase 3: Presentation 계층 — Provider에 `updateChannel` 추가

- [x] `lib/features/channels/presentation/providers/channel_providers.dart`
  - [x] `updateChannel({required int id, String? displayName, String? credentialPlaintext, String? targetIdentifier})` 메서드 추가
  - [x] 시작 시 `isActing: true, clearError: true` 상태 전환
  - [x] `repository.updateChannel(...)` 호출 후 `load()` 재조회
  - [x] 성공 시 `isActing: false, successMessage: '채널이 수정되었습니다.'` 반환 `true`
  - [x] 실패 시 `isActing: false, error: e.toString()` 반환 `false`

---

## Phase 4: UI — `_ChannelCard`에 수정·삭제 진입점 추가

- [x] `lib/features/channels/presentation/screens/channels_screen.dart`

### 4-1. `_ChannelCard` 헤더에 `PopupMenuButton` 추가
  - [x] 기존 Switch + IconButton(테스트) Row에 `PopupMenuButton<String>` 추가
  - [x] 메뉴 항목: `수정` / `삭제`

### 4-2. 삭제 흐름
  - [x] PopupMenu "삭제" 선택 → `showDialog` (AlertDialog)
  - [x] 다이얼로그 본문: "삭제하시겠습니까? 복구할 수 없습니다."
  - [x] [취소] / [삭제(빨간색)] 버튼 구성
  - [x] 확인 시 `notifier.deleteChannel(channel.id)` 호출

### 4-3. 수정 흐름 — `_ChannelEditSheet` 위젯
  - [x] PopupMenu "수정" 선택 → `showModalBottomSheet` 열기
  - [x] `_ChannelEditSheet` private class 구현 (별도 StatefulWidget)
  - [x] `displayName` TextFormField (pre-fill: `channel.displayName`, 필수 입력)
  - [x] 자격증명 TextFormField
    - [x] hint: "변경 시에만 입력 (빈칸이면 기존 유지)"
    - [x] Discord: "Webhook URL", 나머지: "Bot Token"
    - [x] `obscureText: true`
  - [x] `targetIdentifier` TextFormField (pre-fill: `channel.targetIdentifier`)
    - [x] Discord 채널인 경우 숨김 처리
  - [x] [취소] / [저장] 버튼
  - [x] 저장 시 `notifier.updateChannel(...)` 호출
    - [x] `credentialPlaintext`: 빈 문자열이면 `null`로 변환
    - [x] `targetIdentifier`: 빈 문자열이면 `null`로 변환

---

## Phase 5: 검증

### 정적 분석
- [x] `flutter analyze` — 경고 0개 확인 (코드 리뷰로 대체: unused import/variable 없음, exhaustive switch, mounted 체크 정상, .g.dart 최신)

### 수동 테스트 (골든 패스)
- [ ] 채널 카드의 ⋮ 버튼 탭 → 팝업 메뉴 노출 확인
- [ ] "수정" 선택 → BottomSheet 열림, 각 필드 pre-fill 확인
- [ ] `displayName`만 변경 → 저장 → 목록 새로고침 후 반영 확인
- [ ] credential 변경 → 저장 → 테스트 전송으로 동작 확인
- [ ] "삭제" 선택 → 확인 다이얼로그 → 확인 후 목록에서 제거 확인

### 엣지 케이스
- [ ] 삭제 다이얼로그 "취소" 선택 시 채널 유지 확인
- [ ] 수정 시 `displayName` 비우면 저장 불가 (필수 필드 검증)
- [ ] 네트워크 오류 시 SnackBar 에러 메시지 노출 확인
- [ ] Discord 채널 수정 시 `targetIdentifier` 필드 숨김 확인
