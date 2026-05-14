# 채널 ERROR 상태 재연결 버튼 추가

## Context

채널 연결이 5회 실패하면 `ChannelStatus.ERROR` 상태가 되어 자동으로 비활성화된다. 현재 UI의 Switch 토글은 ERROR 상태일 때 `onChanged: null`로 강제 비활성화되어 있어, 화면에서 재활성화할 방법이 없다. DB를 직접 수정해야만 복구 가능한 상황.

백엔드 API(`PATCH /api/v1/channels/{id}/resume`)와 프론트엔드 datasource/repository 레이어는 이미 완성되어 있다. **프론트엔드 Provider + UI만 변경하면 된다.**

---

## 변경 파일

### 1. `channel_providers.dart` — `resumeChannel` 메서드 추가

`frontend/lib/features/channels/presentation/providers/channel_providers.dart`

`sendTest` 메서드(line 90) 다음에 삽입:

```dart
Future<bool> resumeChannel(int id) async {
  state = state.copyWith(isActing: true, clearError: true);
  try {
    await ref.read(channelRepositoryProvider).resumeChannel(id);
    await load();
    state = state.copyWith(
      isActing: false,
      successMessage: '채널 재연결에 성공했습니다.',
    );
    return true;
  } catch (e) {
    state = state.copyWith(isActing: false, error: e.toString());
    return false;
  }
}
```

- `ChannelRepository.resumeChannel(id)` 는 이미 인터페이스에 정의되어 있고 datasource도 구현되어 있음
- `await load()` 로 목록 새로고침 (기존 `toggleStatus`, `createChannel` 패턴과 동일)
- `successMessage` 설정 → `ChannelsScreen`의 `ref.listen`이 자동으로 SnackBar 표시

### 2. `channels_screen.dart` — ERROR 블록에 재연결 버튼 추가

`frontend/lib/features/channels/presentation/screens/channels_screen.dart`

`_ChannelCard.build` 내 ERROR 상태 블록 (line 223-229) 을 변경:

**Before:**
```dart
if (isError && channel.lastError != null) ...[
  const SizedBox(height: AppSpacing.s8),
  Text(
    '${channel.errorCount}회 실패: ${channel.lastError}',
    style: const TextStyle(color: Colors.red, fontSize: 12),
  ),
],
```

**After:**
```dart
if (isError) ...[
  const SizedBox(height: AppSpacing.s8),
  if (channel.lastError != null)
    Text(
      '${channel.errorCount}회 실패: ${channel.lastError}',
      style: const TextStyle(color: Colors.red, fontSize: 12),
    ),
  const SizedBox(height: AppSpacing.s8),
  SizedBox(
    width: double.infinity,
    child: OutlinedButton.icon(
      onPressed: () => ref
          .read(channelNotifierProvider.notifier)
          .resumeChannel(channel.id),
      icon: const Icon(Icons.refresh, size: 16),
      label: const Text('재연결'),
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.error,
        side: const BorderSide(color: AppColors.error),
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.s8),
        textStyle: AppTextStyles.labelLarge,
      ),
    ),
  ),
],
```

- Switch는 ERROR 상태에서 계속 비활성화 유지 (UX 명확성)
- `lastError`가 없어도 ERROR 상태면 버튼 항상 표시
- `OutlinedButton.icon` 패턴은 프로젝트 내 기존 사용 패턴과 동일
- SnackBar 처리는 `ChannelsScreen.ref.listen`이 담당 (별도 코드 불필요)

---

## 변경 범위 요약

| 파일 | 변경 내용 |
|------|-----------|
| `channel_providers.dart` | `resumeChannel(int id)` 메서드 추가 (~15줄) |
| `channels_screen.dart` | ERROR 블록에 재연결 버튼 추가 (7줄 → 22줄) |

백엔드, datasource, repository, model, entity 수정 없음.  
`.g.dart` 재생성 불필요 (클래스 내부 메서드 추가는 codegen 대상 아님).

---

## 검증

1. 채널을 ERROR 상태로 만들기 (백엔드에서 직접 상태 변경 또는 5회 실패 유발)
2. 채널 카드에 빨간 테두리 + "ERROR" 배지 + 에러 메시지 + **"재연결" 버튼** 표시 확인
3. Switch가 비활성화(회색) 상태인지 확인
4. "재연결" 버튼 탭 → `PATCH /api/v1/channels/{id}/resume` 호출 확인
5. 성공: 카드 상태가 ACTIVE로 전환 + "채널 재연결에 성공했습니다." SnackBar 확인
6. `flutter analyze` — 경고 0개 확인
