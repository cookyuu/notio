# 알림 목업 데이터 정보

## 📱 현재 더미 데이터: 1개의 알림

### 알림 상세 정보

```json
{
  "id": 1,
  "source": "SLACK",
  "title": "#dev-team 채널에 멘션",
  "body": "@cookyuu 안녕하세요! PR #456 리뷰 부탁드립니다. 알림 기능 구현 잘 되어가시나요? 👀",
  "priority": "HIGH",
  "is_read": false,
  "created_at": "5초 전",
  "external_id": "slack-msg-20260406-001",
  "external_url": "https://notio-team.slack.com/archives/C123456/p1712421234",
  "metadata": {
    "channel": "dev-team",
    "user": "박철수",
    "user_id": "U123456",
    "has_attachments": false,
    "thread_ts": "1712421234.123456"
  }
}
```

### 시나리오

**상황**: 개발팀 Slack 채널에 PR 리뷰 요청 알림이 방금 도착했습니다.

- 🔔 **소스**: Slack (#dev-team 채널)
- 📬 **상태**: 미읽음 (빨간 점 표시)
- ⚠️ **우선순위**: HIGH (높음)
- ⏰ **도착 시간**: 5초 전 (실시간 알림 시뮬레이션)
- 👤 **발신자**: 박철수
- 🔗 **바로가기**: Slack 메시지 링크 포함

### UI 표시 예시

```
┌─────────────────────────────────────────────┐
│  🔴 SLACK                            5초 전   │
│  ──────────────────────────────────────────  │
│  #dev-team 채널에 멘션                 HIGH  │
│                                              │
│  @cookyuu 안녕하세요! PR #456 리뷰 부탁    │
│  드립니다. 알림 기능 구현 잘 되어가시나요? 👀│
│                                              │
│  👤 박철수 • dev-team                        │
│  ────────────────────────────────────────    │
│  ←  할일 생성    상세보기  →                  │
└─────────────────────────────────────────────┘
```

### 미읽음 카운트

- 총 알림: **1개**
- 미읽음: **1개** 🔴

### 테스트 통과 ✅

```bash
✅ 알림 정보:
   ID: 1
   소스: SLACK
   제목: #dev-team 채널에 멘션
   내용: @cookyuu 안녕하세요! PR #456 리뷰 부탁드립니다...
   우선순위: HIGH
   읽음 여부: 미읽음
   생성 시간: 2026-04-06 21:39:16.658370
   경과 시간: 5초 전
   채널: dev-team
   발신자: 박철수
   외부 링크: https://notio-team.slack.com/...

✅ 미읽음 알림 개수: 1
✅ All tests passed!
```

## 🔧 더미 데이터 확장 방법

더 많은 알림을 테스트하고 싶다면, 다음 파일을 수정하세요:

**파일**: `lib/features/notification/data/datasource/notification_remote_datasource.dart`

**위치**: `_getMockNotifications()` 메서드 내부

```dart
final allMockData = [
  // 여기에 NotificationModel 추가
  NotificationModel(
    id: 2,
    source: 'GITHUB',
    title: '새 PR 생성됨',
    body: '...',
    priority: 'MEDIUM',
    isRead: false,
    createdAt: DateTime.now().subtract(Duration(minutes: 10)).toIso8601String(),
    // ...
  ),
];
```

## 📊 소스별 색상

- **SLACK**: `#FB923C` (오렌지)
- **GITHUB**: `#94A3B8` (회색)
- **CLAUDE**: `#A78BFA` (보라)
- **GMAIL**: `#F87171` (빨강)

## 🎨 우선순위 표시

- **URGENT**: 🔴 빨간색 강조
- **HIGH**: 🟠 주황색
- **MEDIUM**: 🟡 노란색
- **LOW**: ⚪ 회색

## 💡 사용 예시

### Repository에서 사용

```dart
final repository = NotificationRepositoryImpl(
  remoteDataSource: NotificationRemoteDataSource(dio),
  localDataSource: NotificationLocalDataSource(),
);

// 알림 목록 가져오기
final notifications = await repository.fetchNotifications();
// Result: 1개의 Slack 알림

// 미읽음 개수 가져오기
final unreadCount = await repository.getUnreadCount();
// Result: 1
```

### 필터링

```dart
// Slack 알림만 가져오기
final slackNotifications = await repository.fetchNotifications(
  source: NotificationSource.slack,
);

// GitHub 알림만 가져오기 (현재는 0개)
final githubNotifications = await repository.fetchNotifications(
  source: NotificationSource.github,
);
```
