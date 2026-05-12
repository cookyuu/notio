# Plan: Frontend 개발 (Flutter Web)

> **대상 버전**: v2.1
> **작성일**: 2026-05-12
> **연관 Blueprint**: `docs/blueprint/notio_blueprint_v2.md` §9

---

## 개요

v2.1 프론트엔드 변경의 핵심:
1. **`features/chat/` 전체 삭제** — AI 인터랙티브 채팅 제거
2. **`features/delivery_feed/` 신규 구현** — 채널 전달 피드 (채팅 버블 스타일)
3. **`features/channels/` 신규 구현** — Slack/Telegram/Discord 채널 관리 + 라우팅 규칙
4. **네비게이션 변경** — "Chat" → "Deliveries", Settings에 채널관리 메뉴 추가
5. **Flutter Web 전용 의존성 정리** — FCM/sqlite3 관련 패키지 제거

---

## Phase 1: 패키지 제거 및 pubspec 정리

### 1-1. `pubspec.yaml`에서 제거할 패키지

```yaml
# 제거
firebase_core: ^2.27.2
firebase_messaging: ^14.7.20
flutter_local_notifications: ^17.2.3
sqlite3_flutter_libs: ^0.5.0
```

**제거 후 실행:**
```bash
flutter pub get
flutter analyze  # 오류 없어야 함
```

### 1-2. 삭제할 파일 목록

**`features/chat/` 전체:**
```
lib/features/chat/
├── data/
│   ├── datasources/chat_local_datasource.dart
│   ├── datasources/chat_mock_data.dart
│   ├── datasources/chat_remote_datasource.dart
│   └── models/
│       ├── chat_message_model.dart
│       ├── chat_request.dart
│       ├── chat_response.dart
│       └── daily_summary_model.dart
├── data/repository/chat_repository_impl.dart
├── domain/
│   ├── entities/chat_message_entity.dart
│   ├── entities/message_role.dart
│   └── repository/chat_repository.dart
└── presentation/
    ├── providers/
    │   ├── chat_notifier.dart
    │   ├── chat_providers.dart
    │   └── chat_state.dart
    ├── screens/chat_screen.dart
    └── widgets/
        ├── chat_input_field.dart
        ├── chat_message_bubble.dart
        ├── daily_summary_card.dart
        └── streaming_message_bubble.dart
```

**Drift 관련:**
```
lib/core/database/tables/chat_message_table.dart
```
→ `AppDatabase`에서 `ChatMessageTable` 참조 및 `chatMessages` getter 제거.

**`LocalNotificationService`** — FCM 관련 초기화 코드 제거:
```dart
// 제거 대상 코드 (main.dart 또는 LocalNotificationService):
await Firebase.initializeApp();
FirebaseMessaging.onMessage.listen(...);
FirebaseMessaging.onMessageOpenedApp.listen(...);
```

---

## Phase 2: `features/delivery_feed/` 신규 구현

`/chat` 라우트를 재사용. `features/delivery_feed/` 디렉토리로 생성.

### 2-1. 데이터 모델

**`delivery_feed_item_model.dart`:**
```dart
@JsonSerializable()
class DeliveryFeedItemModel {
  final int deliveryLogId;
  final int notificationId;
  final String notificationTitle;
  final int channelId;
  final String channelType;       // "SLACK" | "TELEGRAM" | "DISCORD"
  final String channelDisplayName;
  final String deliveredContent;
  final String deliveredAt;        // ISO 8601
  final String status;
  final String? externalMessageId;

  factory DeliveryFeedItemModel.fromJson(Map<String, dynamic> json) =>
      _$DeliveryFeedItemModelFromJson(json);
}
```

**`delivery_feed_item_entity.dart`:**
```dart
class DeliveryFeedItemEntity {
  final int deliveryLogId;
  final int notificationId;
  final String notificationTitle;
  final int channelId;
  final ChannelTypeEnum channelType;
  final String channelDisplayName;
  final String deliveredContent;
  final DateTime deliveredAt;
  final String status;
  final String? externalMessageId;
}

enum ChannelTypeEnum { slack, telegram, discord }
```

### 2-2. 원격 데이터소스

**`delivery_feed_remote_datasource.dart`:**
```dart
abstract class DeliveryFeedRemoteDataSource {
  Future<PaginatedResult<DeliveryFeedItemModel>> getFeed({
    required int page,
    required int size,
    String? channelType,
  });
}

class DeliveryFeedRemoteDataSourceImpl implements DeliveryFeedRemoteDataSource {
  final Dio dio;

  @override
  Future<PaginatedResult<DeliveryFeedItemModel>> getFeed({
    required int page,
    required int size,
    String? channelType,
  }) async {
    final queryParams = {
      'page': page,
      'size': size,
      if (channelType != null) 'channelType': channelType,
    };
    final response = await dio.get(
      '/api/v1/channels/delivery-feed',
      queryParameters: queryParams,
    );
    // ApiResponse<Page<DeliveryFeedItem>> 파싱
    return PaginatedResult.fromJson(
      response.data['data'],
      (json) => DeliveryFeedItemModel.fromJson(json as Map<String, dynamic>),
    );
  }
}
```

### 2-3. Repository

**`delivery_feed_repository.dart` (인터페이스):**
```dart
abstract class DeliveryFeedRepository {
  Future<PaginatedResult<DeliveryFeedItemEntity>> getFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  });
}
```

### 2-4. State / Notifier

**`delivery_feed_state.dart`:**
```dart
class DeliveryFeedState {
  final List<DeliveryFeedItemEntity> items;
  final bool isLoading;
  final bool isLoadingMore;
  final bool hasMore;
  final int page;
  final ChannelTypeEnum? filter;   // null = All
  final String? error;

  const DeliveryFeedState({
    this.items = const [],
    this.isLoading = false,
    this.isLoadingMore = false,
    this.hasMore = true,
    this.page = 0,
    this.filter,
    this.error,
  });

  DeliveryFeedState copyWith({...}) => ...;
}
```

**`delivery_feed_notifier.dart`:**
```dart
@riverpod
class DeliveryFeedNotifier extends _$DeliveryFeedNotifier {
  @override
  DeliveryFeedState build() => const DeliveryFeedState();

  Future<void> load() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final result = await ref.read(deliveryFeedRepositoryProvider).getFeed(
        page: 0, size: 20, channelType: state.filter,
      );
      state = state.copyWith(
        items: result.content,
        hasMore: !result.last,
        page: 0,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  Future<void> loadMore() async {
    if (!state.hasMore || state.isLoadingMore) return;
    state = state.copyWith(isLoadingMore: true);
    try {
      final result = await ref.read(deliveryFeedRepositoryProvider).getFeed(
        page: state.page + 1, size: 20, channelType: state.filter,
      );
      state = state.copyWith(
        items: [...state.items, ...result.content],
        hasMore: !result.last,
        page: state.page + 1,
        isLoadingMore: false,
      );
    } catch (e) {
      state = state.copyWith(isLoadingMore: false);
    }
  }

  void setFilter(ChannelTypeEnum? filter) {
    state = state.copyWith(filter: filter, items: [], page: 0, hasMore: true);
    load();
  }
}
```

### 2-5. DeliveryFeedScreen

```dart
class DeliveryFeedScreen extends ConsumerStatefulWidget {
  const DeliveryFeedScreen({super.key});

  @override
  ConsumerState<DeliveryFeedScreen> createState() => _DeliveryFeedScreenState();
}

class _DeliveryFeedScreenState extends ConsumerState<DeliveryFeedScreen> {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(deliveryFeedNotifierProvider.notifier).load();
    });
    _scrollController.addListener(_onScroll);
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent * 0.8) {
      ref.read(deliveryFeedNotifierProvider.notifier).loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(deliveryFeedNotifierProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Deliveries'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () =>
                ref.read(deliveryFeedNotifierProvider.notifier).load(),
          ),
        ],
      ),
      body: Column(
        children: [
          ChannelFilterChips(
            selected: state.filter,
            onSelected: (filter) =>
                ref.read(deliveryFeedNotifierProvider.notifier).setFilter(filter),
          ),
          Expanded(child: _buildBody(state)),
        ],
      ),
    );
  }

  Widget _buildBody(DeliveryFeedState state) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('오류가 발생했습니다: ${state.error}'),
            TextButton(
              onPressed: () =>
                  ref.read(deliveryFeedNotifierProvider.notifier).load(),
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }
    if (state.items.isEmpty) {
      return const _EmptyFeedState();
    }
    return RefreshIndicator(
      onRefresh: () => ref.read(deliveryFeedNotifierProvider.notifier).load(),
      child: ListView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        itemCount: state.items.length + (state.isLoadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == state.items.length) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: CircularProgressIndicator(),
              ),
            );
          }
          return DeliveryBubble(
            item: state.items[index],
            onTap: () => context.push(
              '/notifications/${state.items[index].notificationId}',
            ),
          );
        },
      ),
    );
  }
}

class _EmptyFeedState extends StatelessWidget {
  const _EmptyFeedState();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.send_outlined, size: 64, color: Colors.grey),
          const SizedBox(height: 16),
          const Text(
            '전달된 알림이 없습니다.',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          const Text(
            '설정 → 채널 관리에서 Slack, Telegram, Discord 채널을 추가하세요.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey),
          ),
          const SizedBox(height: 16),
          ElevatedButton(
            onPressed: () => context.push('/channels'),
            child: const Text('채널 관리로 이동'),
          ),
        ],
      ),
    );
  }
}
```

### 2-6. DeliveryBubble 위젯

```dart
class DeliveryBubble extends StatelessWidget {
  final DeliveryFeedItemEntity item;
  final VoidCallback onTap;

  const DeliveryBubble({super.key, required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _ChannelAvatar(channelType: item.channelType),
            const SizedBox(width: 10),
            Expanded(
              child: GlassCard(  // 기존 GlassCard 위젯 재사용
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            item.channelDisplayName,
                            style: const TextStyle(
                              fontSize: 12, color: Colors.grey,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            timeago.format(item.deliveredAt, locale: 'ko'),
                            style: const TextStyle(
                              fontSize: 11, color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        item.notificationTitle,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: Colors.white70,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        item.deliveredContent,
                        style: const TextStyle(fontSize: 14, height: 1.4),
                        maxLines: 5,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChannelAvatar extends StatelessWidget {
  final ChannelTypeEnum channelType;
  const _ChannelAvatar({required this.channelType});

  @override
  Widget build(BuildContext context) {
    final (icon, color) = switch (channelType) {
      ChannelTypeEnum.slack    => (Icons.chat_bubble, const Color(0xFF4A154B)),
      ChannelTypeEnum.telegram => (Icons.send, const Color(0xFF0088CC)),
      ChannelTypeEnum.discord  => (Icons.headset, const Color(0xFF5865F2)),
    };
    return CircleAvatar(
      radius: 20,
      backgroundColor: color,
      child: Icon(icon, size: 18, color: Colors.white),
    );
  }
}
```

### 2-7. ChannelFilterChips 위젯

```dart
class ChannelFilterChips extends StatelessWidget {
  final ChannelTypeEnum? selected;
  final void Function(ChannelTypeEnum?) onSelected;

  const ChannelFilterChips({
    super.key, required this.selected, required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final filters = [
      (null, 'All', Icons.all_inclusive),
      (ChannelTypeEnum.slack,    'Slack',    Icons.chat_bubble),
      (ChannelTypeEnum.telegram, 'Telegram', Icons.send),
      (ChannelTypeEnum.discord,  'Discord',  Icons.headset),
    ];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: filters.map((f) {
          final (type, label, icon) = f;
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilterChip(
              label: Text(label),
              avatar: Icon(icon, size: 16),
              selected: selected == type,
              onSelected: (_) => onSelected(type),
            ),
          );
        }).toList(),
      ),
    );
  }
}
```

---

## Phase 3: `features/channels/` 신규 구현

### 3-1. 디렉토리 구조

```
lib/features/channels/
├── data/
│   ├── datasource/channel_remote_datasource.dart
│   └── model/
│       ├── notification_channel_model.dart
│       └── routing_rule_model.dart
├── domain/
│   ├── entity/
│   │   ├── notification_channel_entity.dart
│   │   └── routing_rule_entity.dart
│   └── repository/channel_repository.dart
└── presentation/
    ├── providers/
    │   ├── channel_providers.dart
    │   └── routing_rule_providers.dart
    └── screens/
        ├── channels_screen.dart
        ├── channel_create_screen.dart
        └── routing_rules_screen.dart
```

### 3-2. 도메인 엔티티

**`notification_channel_entity.dart`:**
```dart
class NotificationChannelEntity {
  final int id;
  final ChannelTypeEnum channelType;
  final String displayName;
  final String keyPreview;      // Bot Token 마지막 4자리
  final ChannelStatusEnum status;
  final int errorCount;
  final String? lastError;
  final DateTime? lastDeliveredAt;
}

enum ChannelStatusEnum { active, paused, error }
```

**`routing_rule_entity.dart`:**
```dart
class RoutingRuleEntity {
  final int id;
  final String ruleName;
  final int priorityOrder;
  final RoutingConditionEntity conditions;
  final List<int> channelIds;
  final bool stopOnMatch;
  final bool isEnabled;
  final DeliveryModeEnum deliveryMode;
  final int? digestIntervalMin;
}

enum DeliveryModeEnum { immediate, digest }
```

### 3-3. channels_screen

**주요 기능:**
- 채널 카드 리스트: 이름, 타입 아이콘, 상태 배지(ACTIVE=초록/PAUSED=회색/ERROR=빨강)
- 오류 채널: 빨간 테두리 + `errorCount`회 실패 표시
- 활성화/일시중지 스위치 토글
- 테스트 전송 버튼 → `POST /api/v1/channels/{id}/test`
- FAB → `channel_create_screen.dart`로 이동

```dart
// 채널 카드 핵심 구조
class _ChannelCard extends ConsumerWidget {
  final NotificationChannelEntity channel;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isError = channel.status == ChannelStatusEnum.error;
    return Card(
      shape: isError
          ? RoundedRectangleBorder(
              side: const BorderSide(color: Colors.red, width: 1.5),
              borderRadius: BorderRadius.circular(12),
            )
          : null,
      child: ListTile(
        leading: _ChannelTypeIcon(type: channel.channelType),
        title: Text(channel.displayName),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _StatusBadge(status: channel.status),
            if (isError)
              Text('${channel.errorCount}회 실패: ${channel.lastError}',
                  style: const TextStyle(color: Colors.red, fontSize: 12)),
            if (channel.lastDeliveredAt != null)
              Text('마지막 전달: ${timeago.format(channel.lastDeliveredAt!)}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Switch(
              value: channel.status == ChannelStatusEnum.active,
              onChanged: (val) => ref.read(channelNotifierProvider.notifier)
                  .toggleStatus(channel.id, val),
            ),
            IconButton(
              icon: const Icon(Icons.send_outlined),
              tooltip: '테스트 전송',
              onPressed: () => ref.read(channelNotifierProvider.notifier)
                  .sendTest(channel.id),
            ),
          ],
        ),
      ),
    );
  }
}
```

### 3-4. channel_create_screen

**3단계 폼:**

1. **Step 1**: 채널 타입 선택 (Slack / Telegram / Discord 카드 선택)

2. **Step 2**: 자격증명 입력
   - Slack: Bot Token (`xoxb-...`) + Channel ID (`C0...`)
   - Telegram: Bot Token + Chat ID
   - Discord: Webhook URL

3. **Step 3**: 실시간 검증 → 저장
   - "검증 중..." 스피너 → 성공/실패 피드백
   - 성공 시 `POST /api/v1/channels` 저장 → 목록으로 복귀

```dart
// 채널 생성 요청 모델
class CreateChannelRequest {
  final String displayName;
  final String channelType;
  final String credentialPlaintext;
  final String? targetIdentifier;
}
```

### 3-5. routing_rules_screen

**주요 기능:**
- 규칙 목록 (`priority_order` 오름차순)
- `ReorderableListView` — drag-to-reorder (우선순위 변경)
- 규칙 카드: 규칙명, 조건 칩(소스/우선순위), 대상 채널명, 전달 방식 배지
- FAB → 규칙 추가 바텀시트
- 스와이프 삭제

**규칙 추가/편집 바텀시트:**
```dart
class _RoutingRuleForm extends ConsumerStatefulWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 1. 규칙 이름
        TextFormField(label: '규칙 이름'),

        // 2. 소스 선택 (멀티셀렉트 필터 칩)
        _MultiSelectChips(
          options: ['CLAUDE', 'CODEX', 'GITHUB', 'SLACK', 'GMAIL'],
          label: '소스 (빈 값 = 전체)',
        ),

        // 3. 우선순위 선택 (멀티셀렉트)
        _MultiSelectChips(
          options: ['URGENT', 'HIGH', 'MEDIUM', 'LOW'],
          label: '우선순위 (빈 값 = 전체)',
        ),

        // 4. 대상 채널 선택 (멀티셀렉트)
        _ChannelMultiSelect(),

        // 5. stop_on_match 토글
        SwitchListTile(
          title: const Text('이 규칙 매칭 후 중단'),
          subtitle: const Text('true: 다음 규칙 평가 안 함'),
        ),

        // 6. 전달 방식 선택
        const Text('전달 방식'),
        SegmentedButton<DeliveryModeEnum>(
          segments: const [
            ButtonSegment(value: DeliveryModeEnum.immediate, label: Text('즉시 전송')),
            ButtonSegment(value: DeliveryModeEnum.digest,    label: Text('묶음 전송')),
          ],
          selected: {_selectedMode},
          onSelectionChanged: (modes) => setState(() => _selectedMode = modes.first),
        ),

        // 7. 묶음 간격 (DIGEST 선택 시만 표시)
        if (_selectedMode == DeliveryModeEnum.digest)
          SegmentedButton<int>(
            segments: const [
              ButtonSegment(value: 10,  label: Text('10분')),
              ButtonSegment(value: 20,  label: Text('20분')),
              ButtonSegment(value: 30,  label: Text('30분')),
              ButtonSegment(value: 60,  label: Text('1시간')),
            ],
            selected: {_digestInterval},
            onSelectionChanged: (v) => setState(() => _digestInterval = v.first),
          ),

        ElevatedButton(onPressed: _submit, child: const Text('저장')),
      ],
    );
  }
}
```

---

## Phase 4: 라우터 및 네비게이션 변경

### 4-1. `routes.dart` 변경

```dart
class Routes {
  // 기존 유지
  static const String notifications = '/notifications';
  static const String chat          = '/chat';           // delivery feed 재사용
  static const String analytics     = '/analytics';
  static const String settings      = '/settings';
  static const String connections   = '/settings/connections';

  // 신규 추가
  static const String channels      = '/channels';
  static const String routingRules  = '/routing-rules';
}
```

### 4-2. `app_router.dart` 변경

ShellRoute 내부 변경:
```dart
// 기존: route: '/chat', builder: (_,_) => const ChatScreen()
// 변경:
GoRoute(
  path: Routes.chat,
  builder: (context, state) => const DeliveryFeedScreen(),
),
```

ShellRoute 외부에 추가 (바텀 네비 없이 표시):
```dart
GoRoute(
  path: Routes.channels,
  builder: (context, state) => const ChannelsScreen(),
  routes: [
    GoRoute(
      path: 'create',
      builder: (context, state) => const ChannelCreateScreen(),
    ),
  ],
),
GoRoute(
  path: Routes.routingRules,
  builder: (context, state) => const RoutingRulesScreen(),
),
```

### 4-3. `_BottomNavBar` 변경

```dart
// 기존 탭 1: Chat (Icons.chat)
// 변경:
NavigationDestination(
  icon: const Icon(Icons.send_outlined),
  selectedIcon: const Icon(Icons.send),
  label: 'Deliveries',
),
```

인덱스 매핑:
```dart
final _routes = [
  Routes.notifications,  // 0
  Routes.chat,           // 1 (DeliveryFeedScreen)
  Routes.analytics,      // 2
  Routes.settings,       // 3
];
```

### 4-4. `settings_screen.dart` 변경

기존 "연동 관리" 아래에 두 항목 추가:
```dart
// 기존
ListTile(
  leading: const Icon(Icons.link),
  title: const Text('연동 관리'),
  trailing: const Icon(Icons.chevron_right),
  onTap: () => context.push(Routes.connections),
),

// 신규 추가
ListTile(
  leading: const Icon(Icons.send_outlined),
  title: const Text('채널 관리'),
  subtitle: const Text('Slack, Telegram, Discord'),
  trailing: const Icon(Icons.chevron_right),
  onTap: () => context.push(Routes.channels),
),
ListTile(
  leading: const Icon(Icons.rule),
  title: const Text('라우팅 규칙'),
  subtitle: const Text('알림 전달 조건 및 타이밍 설정'),
  trailing: const Icon(Icons.chevron_right),
  onTap: () => context.push(Routes.routingRules),
),
```

---

## Phase 5: SSE 실시간 업데이트 (FCM 대체)

```dart
// lib/core/services/realtime_notification_service.dart
class RealtimeNotificationService {
  StreamSubscription? _subscription;

  void connect(String token, WidgetRef ref) {
    if (!kIsWeb) return;  // 웹 전용

    final eventSource = html.EventSource(
      '${AppConfig.baseUrl}/api/v1/notifications/stream',
      withCredentials: true,
    );

    _subscription = eventSource.onMessage.listen((event) {
      // 새 알림 이벤트 → Riverpod provider invalidate
      ref.invalidate(notificationsProvider);
      ref.invalidate(unreadCountProvider);
    });
  }

  void disconnect() {
    _subscription?.cancel();
  }
}
```

---

## Phase 6: Flutter Web 빌드

```bash
cd frontend

# 1. 의존성 정리
flutter pub get
flutter analyze  # 경고 0개 확인

# 2. 코드 생성 (riverpod_generator, json_serializable)
dart run build_runner build --delete-conflicting-outputs

# 3. Web 빌드
flutter build web \
  --release \
  --web-renderer html \
  --dart-define=API_BASE_URL=https://your-domain.com

# 4. 빌드 결과물 확인
ls build/web/  # index.html, main.dart.js, flutter.js 등
```

---

## 구현 순서

| 순서 | 작업 | 비고 |
|------|------|------|
| 1 | `pubspec.yaml` FCM/sqlite3 패키지 제거, `flutter pub get` | — |
| 2 | `features/chat/` 전체 삭제 | — |
| 3 | `core/database/tables/chat_message_table.dart` 삭제, `AppDatabase` 참조 제거 | — |
| 4 | `main.dart` / `LocalNotificationService` FCM 초기화 코드 제거 | — |
| 5 | `features/delivery_feed/` — 데이터 모델 (model, entity) 구현 | — |
| 6 | `features/delivery_feed/` — datasource, repository 구현 | 5 |
| 7 | `features/delivery_feed/` — state, notifier, providers 구현 | 6 |
| 8 | `DeliveryBubble`, `ChannelFilterChips` 위젯 구현 | — |
| 9 | `DeliveryFeedScreen` 구현 | 7, 8 |
| 10 | `routes.dart` — channels, routingRules 상수 추가 | — |
| 11 | `app_router.dart` — ChatScreen → DeliveryFeedScreen 교체, 새 라우트 추가 | 9, 10 |
| 12 | `_BottomNavBar` — "Deliveries" 변경 | 10 |
| 13 | `settings_screen.dart` — 채널관리/라우팅규칙 메뉴 추가 | 10 |
| 14 | `features/channels/` — 데이터 모델, datasource, repository 구현 | — |
| 15 | `features/channels/` — providers, notifier 구현 | 14 |
| 16 | `channels_screen.dart` 구현 | 15 |
| 17 | `channel_create_screen.dart` 구현 | 15 |
| 18 | `routing_rules_screen.dart` — Digest 옵션 포함 구현 | 15 |
| 19 | `RealtimeNotificationService` SSE 구현 | — |
| 20 | `flutter analyze` 경고 0개 확인, `flutter build web` 성공 확인 | 전체 |

---

## 테스트 계획

### Widget 테스트

| 테스트 | 검증 항목 |
|--------|---------|
| `DeliveryFeedScreenTest` | 로딩 상태, 빈 상태(채널관리 이동 버튼), 아이템 목록 렌더링 |
| `DeliveryBubbleTest` | 채널 타입별 아이콘 색상, 타임스탬프 포맷, 탭 콜백 |
| `ChannelFilterChipsTest` | 필터 선택/해제, All 선택 시 null 전달 |
| `ChannelsScreenTest` | 오류 채널 빨간 테두리, 토글 스위치 상태 |
| `RoutingRulesFormTest` | IMMEDIATE 선택 시 Digest 간격 숨김, DIGEST 선택 시 간격 표시 |

### 통합 플로우 테스트 (수동)

| 시나리오 | 예상 결과 |
|---------|---------|
| 설정 → 채널 관리 → Slack 채널 추가 | 유효 Token → 채널 저장됨 |
| 설정 → 라우팅 규칙 → 묶음 20분 규칙 추가 | 규칙 목록에 "묶음 20분" 배지 표시 |
| 알림 수신 → Deliveries 탭 | 새 버블 표시, 채널 아이콘 정상 |
| Deliveries 탭 → Discord 필터 선택 | Discord 전달 항목만 표시 |
| 버블 탭 | 알림 상세 화면(`/notifications/:id`)으로 이동 |
| 빈 피드 → "채널 관리로 이동" 버튼 | `/channels` 화면으로 이동 |
