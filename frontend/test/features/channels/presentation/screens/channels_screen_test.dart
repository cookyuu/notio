import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/theme/app_theme.dart';
import 'package:notio_app/features/channels/domain/entity/notification_channel_entity.dart';
import 'package:notio_app/features/channels/domain/entity/routing_rule_entity.dart';
import 'package:notio_app/features/channels/domain/repository/channel_repository.dart';
import 'package:notio_app/features/channels/presentation/providers/channel_providers.dart';
import 'package:notio_app/features/channels/presentation/screens/channels_screen.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

class _FakeChannelRepository implements ChannelRepository {
  _FakeChannelRepository({this.channels = const []});
  final List<NotificationChannelEntity> channels;

  @override
  Future<List<NotificationChannelEntity>> fetchChannels() async => channels;

  @override
  Future<NotificationChannelEntity> createChannel({
    required String displayName,
    required String channelType,
    required String credentialPlaintext,
    String? targetIdentifier,
  }) =>
      throw UnimplementedError();

  @override
  Future<void> deleteChannel(int id) => throw UnimplementedError();

  @override
  Future<void> pauseChannel(int id) async {}

  @override
  Future<void> resumeChannel(int id) async {}

  @override
  Future<void> testChannel(int id) async {}

  @override
  Future<List<RoutingRuleEntity>> fetchRoutingRules() async => [];

  @override
  Future<RoutingRuleEntity> createRoutingRule({
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required bool isEnabled,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  }) =>
      throw UnimplementedError();

  @override
  Future<RoutingRuleEntity> updateRoutingRule({
    required int id,
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required bool isEnabled,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  }) =>
      throw UnimplementedError();

  @override
  Future<void> deleteRoutingRule(int id) => throw UnimplementedError();

  @override
  Future<void> reorderRoutingRules(List<int> orderedIds) =>
      throw UnimplementedError();
}

NotificationChannelEntity _makeChannel({
  required int id,
  required ChannelStatusEnum status,
  String displayName = 'Test Channel',
  ChannelTypeEnum channelType = ChannelTypeEnum.slack,
  int errorCount = 0,
  String? lastError,
}) {
  return NotificationChannelEntity(
    id: id,
    channelType: channelType,
    displayName: displayName,
    status: status,
    errorCount: errorCount,
    createdAt: DateTime(2026, 1, 1),
    lastError: lastError,
  );
}

Future<void> _pumpScreen(
  WidgetTester tester, {
  required List<NotificationChannelEntity> channels,
}) async {
  final container = ProviderContainer(
    overrides: [
      channelRepositoryProvider.overrideWith(
        (ref) => _FakeChannelRepository(channels: channels),
      ),
    ],
  );
  addTearDown(container.dispose);

  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        theme: AppTheme.darkTheme,
        home: const ChannelsScreen(),
      ),
    ),
  );

  await tester.pump(); // postFrameCallback 실행
  await tester.pumpAndSettle(); // 로드 완료
}

void main() {
  group('ChannelsScreen', () {
    group('오류 채널 빨간 테두리', () {
      testWidgets('error channel card has red border with width 1.5',
          (tester) async {
        final errorChannel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.error,
          errorCount: 3,
          lastError: 'Connection refused',
        );

        await _pumpScreen(tester, channels: [errorChannel]);

        final card = tester.widget<Card>(find.byType(Card).first);
        final shape = card.shape as RoundedRectangleBorder;
        expect(shape.side.color, Colors.red);
        expect(shape.side.width, 1.5);
      });

      testWidgets('active channel card has no red border', (tester) async {
        final activeChannel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.active,
        );

        await _pumpScreen(tester, channels: [activeChannel]);

        final card = tester.widget<Card>(find.byType(Card).first);
        // shape is null for non-error channels (uses default card shape)
        expect(card.shape, isNull);
      });

      testWidgets('error channel shows error count and message', (tester) async {
        final errorChannel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.error,
          errorCount: 5,
          lastError: 'Auth failed',
        );

        await _pumpScreen(tester, channels: [errorChannel]);

        expect(find.textContaining('5회 실패'), findsOneWidget);
        expect(find.textContaining('Auth failed'), findsOneWidget);
      });
    });

    group('토글 스위치 상태', () {
      testWidgets('active channel switch value is true', (tester) async {
        final activeChannel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.active,
        );

        await _pumpScreen(tester, channels: [activeChannel]);

        final switchWidget =
            tester.widget<Switch>(find.byType(Switch).first);
        expect(switchWidget.value, isTrue);
      });

      testWidgets('paused channel switch value is false', (tester) async {
        final pausedChannel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.paused,
        );

        await _pumpScreen(tester, channels: [pausedChannel]);

        final switchWidget =
            tester.widget<Switch>(find.byType(Switch).first);
        expect(switchWidget.value, isFalse);
      });

      testWidgets('error channel switch is disabled (onChanged is null)',
          (tester) async {
        final errorChannel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.error,
        );

        await _pumpScreen(tester, channels: [errorChannel]);

        final switchWidget =
            tester.widget<Switch>(find.byType(Switch).first);
        expect(switchWidget.onChanged, isNull);
      });
    });

    group('빈 상태', () {
      testWidgets('shows empty state when no channels', (tester) async {
        await _pumpScreen(tester, channels: []);

        expect(find.text('등록된 채널이 없습니다'), findsOneWidget);
      });
    });

    group('목록 렌더링', () {
      testWidgets('shows channel display name', (tester) async {
        final channel = _makeChannel(
          id: 1,
          status: ChannelStatusEnum.active,
          displayName: 'Dev Slack',
        );

        await _pumpScreen(tester, channels: [channel]);

        expect(find.text('Dev Slack'), findsOneWidget);
      });

      testWidgets('shows ACTIVE status badge for active channel',
          (tester) async {
        final channel = _makeChannel(id: 1, status: ChannelStatusEnum.active);
        await _pumpScreen(tester, channels: [channel]);
        expect(find.text('ACTIVE'), findsOneWidget);
      });

      testWidgets('shows PAUSED status badge for paused channel',
          (tester) async {
        final channel = _makeChannel(id: 1, status: ChannelStatusEnum.paused);
        await _pumpScreen(tester, channels: [channel]);
        expect(find.text('PAUSED'), findsOneWidget);
      });

      testWidgets('shows ERROR status badge for error channel', (tester) async {
        final channel = _makeChannel(id: 1, status: ChannelStatusEnum.error);
        await _pumpScreen(tester, channels: [channel]);
        expect(find.text('ERROR'), findsOneWidget);
      });

      testWidgets('shows test send button for each channel', (tester) async {
        final channels = [
          _makeChannel(id: 1, status: ChannelStatusEnum.active),
          _makeChannel(id: 2, status: ChannelStatusEnum.paused),
        ];
        await _pumpScreen(tester, channels: channels);

        expect(find.byIcon(Icons.send_outlined), findsNWidgets(2));
      });
    });
  });
}
