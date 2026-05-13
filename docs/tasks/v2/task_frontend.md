# Task: Frontend 개발 체크리스트

> **대상 버전**: v2.1
> **작성일**: 2026-05-12
> **연관 Plan**: `docs/plans/v2/plan_frontend.md`

---

## Phase 1: 패키지 제거 및 정리

### 1-1. pubspec.yaml 패키지 제거
- [x] `firebase_core: ^2.27.2` 제거 (이미 주석처리 상태였음)
- [x] `firebase_messaging: ^14.7.20` 제거 (이미 주석처리 상태였음)
- [x] `flutter_local_notifications: ^17.2.3` 제거
- [x] `sqlite3_flutter_libs: ^0.5.0` 제거
- [x] `flutter pub get` 실행

### 1-2. chat/ 피처 삭제
- [x] `lib/features/chat/data/datasources/chat_local_datasource.dart` 삭제
- [x] `lib/features/chat/data/datasources/chat_mock_data.dart` 삭제
- [x] `lib/features/chat/data/datasources/chat_remote_datasource.dart` 삭제
- [x] `lib/features/chat/data/models/chat_message_model.dart` 삭제
- [x] `lib/features/chat/data/models/chat_request.dart` 삭제
- [x] `lib/features/chat/data/models/chat_response.dart` 삭제
- [x] `lib/features/chat/data/models/daily_summary_model.dart` 삭제
- [x] `lib/features/chat/data/repository/chat_repository_impl.dart` 삭제
- [x] `lib/features/chat/domain/entities/chat_message_entity.dart` 삭제
- [x] `lib/features/chat/domain/entities/message_role.dart` 삭제
- [x] `lib/features/chat/domain/repository/chat_repository.dart` 삭제
- [x] `lib/features/chat/presentation/providers/chat_notifier.dart` 삭제
- [x] `lib/features/chat/presentation/providers/chat_providers.dart` 삭제
- [x] `lib/features/chat/presentation/providers/chat_state.dart` 삭제
- [x] `lib/features/chat/presentation/screens/chat_screen.dart` 삭제
- [x] `lib/features/chat/presentation/widgets/chat_input_field.dart` 삭제
- [x] `lib/features/chat/presentation/widgets/chat_message_bubble.dart` 삭제
- [x] `lib/features/chat/presentation/widgets/daily_summary_card.dart` 삭제
- [x] `lib/features/chat/presentation/widgets/streaming_message_bubble.dart` 삭제

### 1-3. Drift 및 FCM 정리
- [x] `lib/core/database/tables/chat_message_table.dart` 삭제
- [x] `AppDatabase`에서 `ChatMessageTable` 참조 제거
- [x] `AppDatabase`에서 `chatMessages` getter 제거
- [x] `main.dart` / `LocalNotificationService` — `Firebase.initializeApp()` 제거 (원래 코드에 없었음, LocalNotificationService 자체를 삭제)
- [x] `FirebaseMessaging.onMessage.listen()` 제거 (원래 코드에 없었음)
- [x] `FirebaseMessaging.onMessageOpenedApp.listen()` 제거 (원래 코드에 없었음)
- [x] `flutter analyze` — 오류 없음 확인

---

## Phase 2: delivery_feed/ 신규 구현

### 2-1. 데이터 모델
- [ ] `lib/features/delivery_feed/data/model/delivery_feed_item_model.dart` — `@JsonSerializable` 구현
  - [ ] 필드: `deliveryLogId`, `notificationId`, `notificationTitle`, `channelId`, `channelType`, `channelDisplayName`, `deliveredContent`, `deliveredAt`, `status`, `externalMessageId`
- [ ] `lib/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart` 구현
- [ ] `ChannelTypeEnum` — `slack` / `telegram` / `discord` 정의
- [ ] `json_serializable` 코드 생성 (`delivery_feed_item_model.g.dart`)

### 2-2. 데이터소스 및 Repository
- [ ] `lib/features/delivery_feed/data/datasource/delivery_feed_remote_datasource.dart` 구현
  - [ ] `DeliveryFeedRemoteDataSource` 추상 클래스 정의
  - [ ] `DeliveryFeedRemoteDataSourceImpl` — `GET /api/v1/channels/delivery-feed` 구현
  - [ ] `queryParameters`: `page`, `size`, `channelType` (nullable)
  - [ ] `ApiResponse<Page<DeliveryFeedItem>>` 파싱
- [ ] `lib/features/delivery_feed/domain/repository/delivery_feed_repository.dart` 인터페이스 정의
- [ ] `DeliveryFeedRepositoryImpl` 구현 (model → entity 변환 포함)

### 2-3. State / Notifier / Provider
- [ ] `lib/features/delivery_feed/presentation/providers/delivery_feed_state.dart` 구현
  - [ ] 필드: `items`, `isLoading`, `isLoadingMore`, `hasMore`, `page`, `filter`, `error`
  - [ ] `copyWith()` 구현
- [ ] `lib/features/delivery_feed/presentation/providers/delivery_feed_notifier.dart` — `@riverpod` 구현
  - [ ] `load()` — page=0 초기 로드, isLoading 상태 관리
  - [ ] `loadMore()` — hasMore 및 isLoadingMore 중복 호출 방지
  - [ ] `setFilter(ChannelTypeEnum?)` — items 초기화 후 `load()` 호출
- [ ] `dart run build_runner build --delete-conflicting-outputs` 실행

### 2-4. 위젯 구현
- [ ] `lib/features/delivery_feed/presentation/widgets/delivery_bubble.dart` 구현
  - [ ] 기존 `GlassCard` 위젯 재사용
  - [ ] 상단: 채널 표시명(좌), 전달 시각 `timeago.format(locale: 'ko')`(우)
  - [ ] 알림 제목 (fontWeight: w600, color: white70)
  - [ ] 전달 내용 (`maxLines: 5`, `overflow: TextOverflow.ellipsis`)
  - [ ] 탭 → `onTap` 콜백 실행
- [ ] `_ChannelAvatar` 위젯 — 채널 타입별 아이콘/색상
  - [ ] Slack: `Icons.chat_bubble`, `Color(0xFF4A154B)`
  - [ ] Telegram: `Icons.send`, `Color(0xFF0088CC)`
  - [ ] Discord: `Icons.headset`, `Color(0xFF5865F2)`
- [ ] `lib/features/delivery_feed/presentation/widgets/channel_filter_chips.dart` 구현
  - [ ] All / Slack / Telegram / Discord FilterChip 4개
  - [ ] 수평 스크롤 (`SingleChildScrollView`)
  - [ ] 선택 상태 표시

### 2-5. DeliveryFeedScreen 구현
- [ ] `lib/features/delivery_feed/presentation/screens/delivery_feed_screen.dart` 구현
- [ ] `initState`에서 `load()` 호출 (`addPostFrameCallback`)
- [ ] `ScrollController` — 80% 도달 시 `loadMore()` 호출
- [ ] `ChannelFilterChips` — 상단 고정, 선택 시 `setFilter()` 호출
- [ ] 로딩 상태: `CircularProgressIndicator` 중앙
- [ ] 오류 상태: 오류 메시지 + 재시도 버튼
- [ ] 빈 상태: `_EmptyFeedState`
  - [ ] `Icons.send_outlined` (size 64, grey)
  - [ ] "채널 관리로 이동" `ElevatedButton` → `context.push('/channels')`
- [ ] `RefreshIndicator` 당겨서 새로고침
- [ ] `ListView.builder` 하단 isLoadingMore 인디케이터
- [ ] 버블 탭 → `/notifications/{notificationId}` 이동

---

## Phase 3: channels/ 신규 구현

### 3-1. 데이터 모델 및 엔티티
- [ ] `lib/features/channels/data/model/notification_channel_model.dart` — `@JsonSerializable` 구현
- [ ] `lib/features/channels/data/model/routing_rule_model.dart` — `@JsonSerializable` 구현
- [ ] `lib/features/channels/domain/entity/notification_channel_entity.dart` 구현
  - [ ] `ChannelStatusEnum` — `active` / `paused` / `error`
- [ ] `lib/features/channels/domain/entity/routing_rule_entity.dart` 구현
  - [ ] `DeliveryModeEnum` — `immediate` / `digest`
  - [ ] `RoutingConditionEntity` (sources, priorities 목록)

### 3-2. 데이터소스 및 Repository
- [ ] `lib/features/channels/data/datasource/channel_remote_datasource.dart` 구현
  - [ ] 채널 CRUD API 호출
  - [ ] `PATCH /channels/{id}/pause`, `PATCH /channels/{id}/resume`
  - [ ] `POST /channels/{id}/test` — 테스트 전송
  - [ ] 라우팅 규칙 CRUD + `PATCH /routing-rules/reorder`
- [ ] `lib/features/channels/domain/repository/channel_repository.dart` 인터페이스 정의
- [ ] `ChannelRepositoryImpl` 구현 (model → entity 변환 포함)

### 3-3. Providers / Notifier
- [ ] `lib/features/channels/presentation/providers/channel_providers.dart` 구현
  - [ ] 채널 목록 로드 / CRUD
  - [ ] `toggleStatus(int id, bool active)` — pause/resume 분기
  - [ ] `sendTest(int id)` — 테스트 전송 + 결과 스낵바
- [ ] `lib/features/channels/presentation/providers/routing_rule_providers.dart` 구현
  - [ ] 규칙 목록 로드 / CRUD
  - [ ] `reorder(List<int> orderedIds)` — drag-to-reorder 후 API 호출

### 3-4. ChannelsScreen 구현
- [ ] `lib/features/channels/presentation/screens/channels_screen.dart` 구현
- [ ] `_ChannelCard` 위젯 구현
  - [ ] 오류 채널: `BorderSide(color: Colors.red, width: 1.5)` 테두리
  - [ ] `_StatusBadge` — ACTIVE(초록) / PAUSED(회색) / ERROR(빨강)
  - [ ] `errorCount`회 실패 + `lastError` 메시지 표시
  - [ ] `lastDeliveredAt` → `timeago.format()` 표시
  - [ ] `Switch` 토글 → `toggleStatus()` 호출
  - [ ] 테스트 전송 `IconButton` → `sendTest()` 호출
- [ ] FAB → `ChannelCreateScreen`으로 이동 (`context.push('/channels/create')`)

### 3-5. ChannelCreateScreen 구현
- [ ] `lib/features/channels/presentation/screens/channel_create_screen.dart` 구현
- [ ] Step 1: 채널 타입 선택 카드 (Slack / Telegram / Discord)
- [ ] Step 2: 자격증명 입력 폼
  - [ ] Slack: Bot Token (`xoxb-...`) + Channel ID (`C0...`)
  - [ ] Telegram: Bot Token + Chat ID
  - [ ] Discord: Webhook URL 전체
- [ ] Step 3: "검증 중..." 스피너 → 성공/실패 피드백
  - [ ] 성공 시 `POST /api/v1/channels` 저장 → 목록으로 복귀

### 3-6. RoutingRulesScreen 구현
- [ ] `lib/features/channels/presentation/screens/routing_rules_screen.dart` 구현
- [ ] `ReorderableListView` — drag-to-reorder, `reorder()` 호출
- [ ] 규칙 카드 — 규칙명, 소스/우선순위 조건 칩, 대상 채널명, 전달 방식 배지
- [ ] FAB → 규칙 추가 바텀시트 (`showModalBottomSheet`)
- [ ] 스와이프 삭제 (`Dismissible` 또는 `flutter_slidable`)
- [ ] `_RoutingRuleForm` 바텀시트 구현
  - [ ] 규칙 이름 `TextFormField`
  - [ ] 소스 멀티셀렉트 `FilterChip` (CLAUDE / CODEX / GITHUB / SLACK / GMAIL)
  - [ ] 우선순위 멀티셀렉트 `FilterChip` (URGENT / HIGH / MEDIUM / LOW)
  - [ ] 대상 채널 멀티셀렉트 (채널 목록 provider 활용)
  - [ ] stop_on_match `SwitchListTile`
  - [ ] 전달 방식 `SegmentedButton<DeliveryModeEnum>` (즉시 / 묶음)
  - [ ] 묶음 간격 `SegmentedButton<int>` (10분 / 20분 / 30분 / 1시간) — DIGEST 선택 시만 표시
  - [ ] "저장" 버튼 → `POST` 또는 `PUT` 후 목록 갱신

---

## Phase 4: 라우터 및 네비게이션 변경

- [ ] `lib/core/router/routes.dart` — `channels = '/channels'` 상수 추가
- [ ] `lib/core/router/routes.dart` — `routingRules = '/routing-rules'` 상수 추가
- [ ] `lib/core/router/app_router.dart` — `/chat` 라우트 → `DeliveryFeedScreen` 교체
- [ ] `app_router.dart` — `/channels` 라우트 추가 (ShellRoute 외부)
  - [ ] 하위 라우트 `/channels/create` 추가
- [ ] `app_router.dart` — `/routing-rules` 라우트 추가 (ShellRoute 외부)
- [ ] `_BottomNavBar` — 탭 1: 아이콘 `Icons.send_outlined` / `Icons.send`, 라벨 `'Deliveries'` 변경
- [ ] `settings_screen.dart` — "채널 관리" `ListTile` 추가
  - [ ] `subtitle: 'Slack, Telegram, Discord'`
  - [ ] `onTap: () => context.push(Routes.channels)`
- [ ] `settings_screen.dart` — "라우팅 규칙" `ListTile` 추가
  - [ ] `subtitle: '알림 전달 조건 및 타이밍 설정'`
  - [ ] `onTap: () => context.push(Routes.routingRules)`

---

## Phase 5: SSE 실시간 업데이트

- [ ] `lib/core/services/realtime_notification_service.dart` 구현
- [ ] `kIsWeb` 조건부 — 웹 전용 SSE 연결 (`dart:html` 사용)
- [ ] `html.EventSource('/api/v1/notifications/stream', withCredentials: true)` 연결
- [ ] `onMessage` 수신 시 `ref.invalidate(notificationsProvider)` 호출
- [ ] `onMessage` 수신 시 `ref.invalidate(unreadCountProvider)` 호출
- [ ] `disconnect()` — `_subscription?.cancel()` 처리

---

## Phase 6: Flutter Web 빌드 검증

- [ ] `flutter pub get` 실행
- [ ] `flutter analyze` — 경고 0개 확인
- [ ] `dart run build_runner build --delete-conflicting-outputs` 실행
- [ ] `flutter build web --release --web-renderer html --dart-define=API_BASE_URL=...` 성공
- [ ] `build/web/` — `index.html`, `main.dart.js`, `flutter.js` 존재 확인

---

## Phase 7: 테스트

### Widget 테스트
- [ ] `DeliveryFeedScreenTest` — 로딩 상태, 빈 상태(채널관리 이동 버튼), 아이템 목록 렌더링
- [ ] `DeliveryBubbleTest` — 채널 타입별 아이콘 색상, 타임스탬프 포맷, 탭 콜백
- [ ] `ChannelFilterChipsTest` — 필터 선택/해제, All 선택 시 null 전달
- [ ] `ChannelsScreenTest` — 오류 채널 빨간 테두리, 토글 스위치 상태
- [ ] `RoutingRulesFormTest` — IMMEDIATE 선택 시 Digest 간격 숨김, DIGEST 선택 시 간격 표시

### 통합 플로우 테스트 (수동)
- [ ] 설정 → 채널 관리 → Slack 채널 추가 → 유효 Token → 채널 저장됨
- [ ] 설정 → 라우팅 규칙 → 묶음 20분 규칙 추가 → 목록에 "묶음 20분" 배지 표시
- [ ] 알림 수신 → Deliveries 탭 → 새 버블 표시, 채널 아이콘 정상
- [ ] Deliveries 탭 → Discord 필터 선택 → Discord 전달 항목만 표시
- [ ] 버블 탭 → 알림 상세 화면(`/notifications/:id`)으로 이동
- [ ] 빈 피드 → "채널 관리로 이동" 버튼 → `/channels` 화면 이동
