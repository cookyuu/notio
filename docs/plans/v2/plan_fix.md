# 채널 삭제·수정 기능 구현 계획

## Context

채널 관리 화면(`channels_screen.dart`)의 `_ChannelCard`에 현재 토글(활성화/비활성화)과 테스트 전송 버튼만 있고, **삭제·수정 UI가 없다.**
백엔드에는 `PUT /api/v1/channels/{id}` 및 `DELETE /api/v1/channels/{id}`가 모두 구현되어 있으며, 프론트엔드 DataSource·Provider에도 `deleteChannel`은 있으나 `updateChannel`이 없고, UI 진입점도 누락되어 있다.

## 변경 범위 (7개 파일)

### 1. 모델·엔티티에 `targetIdentifier` 추가

백엔드 `ChannelResponse`는 `target_identifier`를 반환하지만 Flutter 모델/엔티티에 미반영.
수정 폼에서 channel ID / chat ID를 pre-fill하기 위해 필드를 추가한다.

**`lib/features/channels/data/model/notification_channel_model.dart`**
- `@JsonKey(name: 'target_identifier') final String? targetIdentifier` 추가
- 생성자에 optional 파라미터 추가
- `toEntity()`에서 해당 필드 전달

**`lib/features/channels/domain/entity/notification_channel_entity.dart`**
- `final String? targetIdentifier` 필드 추가

> ⚠️ 변경 후 `flutter pub run build_runner build --delete-conflicting-outputs` 필수 (`.g.dart` 재생성)

---

### 2. DataSource에 `updateChannel` 추가

**`lib/features/channels/data/datasource/channel_remote_datasource.dart`**

```dart
Future<NotificationChannelModel> updateChannel(
  int id,
  Map<String, dynamic> data,
) async {
  final response = await _dio.put('/api/v1/channels/$id', data: data);
  if (response.data['success'] != true) {
    throw Exception(response.data['error']['message']);
  }
  return NotificationChannelModel.fromJson(response.data['data']);
}
```

---

### 3. Repository 인터페이스에 `updateChannel` 추가

**`lib/features/channels/domain/repository/channel_repository.dart`**

```dart
Future<NotificationChannelEntity> updateChannel({
  required int id,
  String? displayName,
  String? credentialPlaintext,
  String? targetIdentifier,
});
```

---

### 4. Repository 구현체에 `updateChannel` 추가

**`lib/features/channels/data/repository/channel_repository_impl.dart`**

- `updateChannel` 구현: null이 아닌 필드만 Map에 포함해서 전송
```dart
final body = {
  if (displayName != null) 'display_name': displayName,
  if (credentialPlaintext != null) 'credential_plaintext': credentialPlaintext,
  if (targetIdentifier != null) 'target_identifier': targetIdentifier,
};
```

---

### 5. Provider에 `updateChannel` 추가

**`lib/features/channels/presentation/providers/channel_providers.dart`**

```dart
Future<bool> updateChannel({
  required int id,
  String? displayName,
  String? credentialPlaintext,
  String? targetIdentifier,
}) async {
  state = state.copyWith(isActing: true, clearError: true);
  try {
    await ref.read(channelRepositoryProvider).updateChannel(
      id: id,
      displayName: displayName,
      credentialPlaintext: credentialPlaintext,
      targetIdentifier: targetIdentifier,
    );
    await load();
    state = state.copyWith(isActing: false, successMessage: '채널이 수정되었습니다.');
    return true;
  } catch (e) {
    state = state.copyWith(isActing: false, error: e.toString());
    return false;
  }
}
```

---

### 6. UI: `_ChannelCard`에 수정·삭제 버튼 추가

**`lib/features/channels/presentation/screens/channels_screen.dart`**

**6-1. `_ChannelCard` 헤더 Row에 `PopupMenuButton` 추가**
- 기존: Switch + IconButton(테스트)
- 변경: Switch + IconButton(테스트) + `PopupMenuButton<String>`(⋮)
  - 메뉴 항목: `수정`, `삭제`

**6-2. 삭제 흐름**
```
PopupMenu "삭제" 선택
  → showDialog (AlertDialog)
    → "삭제하시겠습니까? 복구할 수 없습니다."
    → [취소] [삭제(빨간색)] 버튼
  → 확인 시 notifier.deleteChannel(channel.id)
```

**6-3. 수정 흐름**
```
PopupMenu "수정" 선택
  → showModalBottomSheet
    → _ChannelEditSheet 위젯 (별도 private class)
      - displayName 필드 (pre-fill: channel.displayName)
      - 자격증명 필드 (빈값, hint: "변경 시에만 입력 (빈칸이면 기존 유지)")
        - Discord: Webhook URL, 나머지: Bot Token
        - obscureText: true
      - targetIdentifier 필드 (pre-fill: channel.targetIdentifier)
        - Discord 채널이면 숨김
      - [취소] [저장] 버튼
    → 저장 시 notifier.updateChannel(...)
      - credentialPlaintext: 빈 문자열이면 null로 변환 (기존 유지)
      - targetIdentifier: 빈 문자열이면 null로 변환
```

---

## 수정되지 않는 파일

- `routing_rules_screen.dart` — 이미 편집·삭제 구현 완료
- `channel_create_screen.dart` — 변경 없음

---

## 검증 방법

1. **코드 생성**: `flutter pub run build_runner build --delete-conflicting-outputs`
2. **정적 분석**: `flutter analyze` → 경고 0개
3. **수동 테스트 (골든 패스)**:
   - 채널 카드의 ⋮ 버튼 탭 → 메뉴 노출 확인
   - "수정" → BottomSheet 열림, 필드 pre-fill 확인
   - displayName만 변경 → 저장 → 목록 새로고침 후 반영 확인
   - credential 변경 → 저장 → 테스트 전송으로 동작 확인
   - "삭제" → 확인 다이얼로그 → 확인 후 목록에서 제거 확인
4. **엣지 케이스**:
   - 삭제 다이얼로그에서 "취소" 선택 시 채널 유지 확인
   - 수정 시 displayName을 비우면 저장 불가 (필수 필드)
   - 네트워크 오류 시 SnackBar 에러 메시지 노출 확인
