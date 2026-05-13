import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/theme/app_theme.dart';
import 'package:notio_app/features/channels/domain/entity/notification_channel_entity.dart';
import 'package:notio_app/features/channels/domain/entity/routing_rule_entity.dart';
import 'package:notio_app/features/channels/domain/repository/channel_repository.dart';
import 'package:notio_app/features/channels/presentation/providers/channel_providers.dart';
import 'package:notio_app/features/channels/presentation/screens/routing_rules_screen.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

class _FakeChannelRepository implements ChannelRepository {
  _FakeChannelRepository({
    this.channels = const [],
    this.rules = const [],
  });

  final List<NotificationChannelEntity> channels;
  final List<RoutingRuleEntity> rules;

  @override
  Future<List<NotificationChannelEntity>> fetchChannels() async => channels;

  @override
  Future<List<RoutingRuleEntity>> fetchRoutingRules() async => rules;

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
  Future<RoutingRuleEntity> createRoutingRule({
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required bool isEnabled,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  }) async =>
      _makeRule(id: 99, deliveryMode: deliveryMode);

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

RoutingRuleEntity _makeRule({
  required int id,
  DeliveryModeEnum deliveryMode = DeliveryModeEnum.immediate,
  int? digestIntervalMin,
}) {
  return RoutingRuleEntity(
    id: id,
    ruleName: 'Rule $id',
    priorityOrder: id,
    conditions: const RoutingConditionEntity(sources: [], priorities: []),
    channelIds: const [],
    stopOnMatch: false,
    isEnabled: true,
    deliveryMode: deliveryMode,
    digestIntervalMin: digestIntervalMin,
  );
}

Future<void> _pumpScreen(
  WidgetTester tester, {
  List<NotificationChannelEntity> channels = const [],
  List<RoutingRuleEntity> rules = const [],
}) async {
  final container = ProviderContainer(
    overrides: [
      channelRepositoryProvider.overrideWith(
        (ref) => _FakeChannelRepository(channels: channels, rules: rules),
      ),
    ],
  );
  addTearDown(container.dispose);

  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        theme: AppTheme.darkTheme,
        home: const RoutingRulesScreen(),
      ),
    ),
  );

  await tester.pump(); // postFrameCallback 실행
  await tester.pumpAndSettle(); // 로드 완료
}

void main() {
  group('RoutingRulesForm — 전달 방식 선택', () {
    testWidgets('IMMEDIATE 선택 시 묶음 간격 섹션이 표시되지 않음', (tester) async {
      await _pumpScreen(tester);

      // FAB 탭 → 바텀시트 열림
      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      // 폼이 열렸는지 확인
      expect(find.text('규칙 추가'), findsOneWidget);

      // 기본 전달 방식은 IMMEDIATE → 묶음 간격 숨김
      expect(find.text('묶음 간격'), findsNothing);
      expect(find.widgetWithText(Text, '10분'), findsNothing);
      expect(find.widgetWithText(Text, '20분'), findsNothing);
    });

    testWidgets('DIGEST 선택 시 묶음 간격 섹션이 표시됨', (tester) async {
      await _pumpScreen(tester);

      // FAB 탭 → 바텀시트 열림
      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      // '묶음 전송' 세그먼트 버튼 탭
      await tester.tap(find.text('묶음 전송'));
      await tester.pumpAndSettle();

      // 묶음 간격 섹션이 나타남
      expect(find.text('묶음 간격'), findsOneWidget);
    });

    testWidgets('DIGEST 선택 시 간격 옵션 버튼이 4개 표시됨', (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      await tester.tap(find.text('묶음 전송'));
      await tester.pumpAndSettle();

      // 간격 버튼: 10분, 20분, 30분, 1시간
      expect(find.text('10분'), findsOneWidget);
      expect(find.text('20분'), findsOneWidget);
      expect(find.text('30분'), findsOneWidget);
      expect(find.text('1시간'), findsOneWidget);
    });

    testWidgets('DIGEST → IMMEDIATE 전환 시 묶음 간격 섹션이 다시 숨겨짐',
        (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      // DIGEST 선택
      await tester.tap(find.text('묶음 전송'));
      await tester.pumpAndSettle();
      expect(find.text('묶음 간격'), findsOneWidget);

      // IMMEDIATE 재선택
      await tester.tap(find.text('즉시 전송'));
      await tester.pumpAndSettle();
      expect(find.text('묶음 간격'), findsNothing);
    });

    testWidgets('폼에 규칙 이름 텍스트 필드가 있음', (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(TextFormField, '규칙 이름'), findsOneWidget);
    });

    testWidgets('폼에 소스 필터칩이 표시됨', (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(FilterChip, 'CLAUDE'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'GITHUB'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'SLACK'), findsOneWidget);
    });

    testWidgets('폼에 우선순위 필터칩이 표시됨', (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(FilterChip, 'URGENT'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'HIGH'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'MEDIUM'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'LOW'), findsOneWidget);
    });

    testWidgets('저장 버튼이 있음', (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(ElevatedButton, '저장'), findsOneWidget);
    });

    testWidgets('전달 방식 SegmentedButton에 즉시 전송과 묶음 전송 옵션이 있음',
        (tester) async {
      await _pumpScreen(tester);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.text('즉시 전송'), findsOneWidget);
      expect(find.text('묶음 전송'), findsOneWidget);
    });
  });
}
