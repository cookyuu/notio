import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';
import 'package:notio_app/features/delivery_feed/presentation/widgets/delivery_bubble.dart';
import 'package:timeago/timeago.dart' as timeago;

DeliveryFeedItemEntity _makeItem({
  ChannelTypeEnum channelType = ChannelTypeEnum.slack,
  String displayName = 'Test Channel',
  String title = 'Test Notification',
  String content = 'Test content',
  DateTime? deliveredAt,
}) {
  return DeliveryFeedItemEntity(
    deliveryLogId: 1,
    notificationId: 42,
    notificationTitle: title,
    channelId: 1,
    channelType: channelType,
    channelDisplayName: displayName,
    deliveredContent: content,
    deliveredAt: deliveredAt ?? DateTime.now().subtract(const Duration(minutes: 5)),
    status: 'delivered',
  );
}

Future<void> _pumpBubble(
  WidgetTester tester,
  DeliveryFeedItemEntity item, {
  VoidCallback? onTap,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: DeliveryBubble(item: item, onTap: onTap ?? () {}),
      ),
    ),
  );
}

void main() {
  setUpAll(() {
    timeago.setLocaleMessages('ko', timeago.KoMessages());
  });

  group('DeliveryBubble', () {
    testWidgets('displays channel display name and notification title',
        (tester) async {
      final item = _makeItem(displayName: 'My Slack', title: 'Build Failed');
      await _pumpBubble(tester, item);

      expect(find.text('My Slack'), findsOneWidget);
      expect(find.text('Build Failed'), findsOneWidget);
    });

    testWidgets('displays delivered content', (tester) async {
      final item = _makeItem(content: 'Deploy succeeded in 3m 24s');
      await _pumpBubble(tester, item);

      expect(find.text('Deploy succeeded in 3m 24s'), findsOneWidget);
    });

    group('채널 타입별 아이콘', () {
      testWidgets('Slack channel shows chat_bubble icon', (tester) async {
        await _pumpBubble(tester, _makeItem(channelType: ChannelTypeEnum.slack));
        expect(find.byIcon(Icons.chat_bubble), findsOneWidget);
      });

      testWidgets('Telegram channel shows send icon', (tester) async {
        await _pumpBubble(
            tester, _makeItem(channelType: ChannelTypeEnum.telegram));
        expect(find.byIcon(Icons.send), findsOneWidget);
      });

      testWidgets('Discord channel shows headset icon', (tester) async {
        await _pumpBubble(
            tester, _makeItem(channelType: ChannelTypeEnum.discord));
        expect(find.byIcon(Icons.headset), findsOneWidget);
      });
    });

    group('채널 타입별 아이콘 색상', () {
      testWidgets('Slack avatar has purple color (0xFF4A154B)', (tester) async {
        await _pumpBubble(
            tester, _makeItem(channelType: ChannelTypeEnum.slack));
        final containers =
            tester.widgetList<Container>(find.byType(Container)).toList();
        final found = containers.any((c) {
          final dec = c.decoration;
          return dec is BoxDecoration && dec.color == const Color(0xFF4A154B);
        });
        expect(found, isTrue);
      });

      testWidgets('Telegram avatar has blue color (0xFF0088CC)', (tester) async {
        await _pumpBubble(
            tester, _makeItem(channelType: ChannelTypeEnum.telegram));
        final containers =
            tester.widgetList<Container>(find.byType(Container)).toList();
        final found = containers.any((c) {
          final dec = c.decoration;
          return dec is BoxDecoration && dec.color == const Color(0xFF0088CC);
        });
        expect(found, isTrue);
      });

      testWidgets('Discord avatar has indigo color (0xFF5865F2)', (tester) async {
        await _pumpBubble(
            tester, _makeItem(channelType: ChannelTypeEnum.discord));
        final containers =
            tester.widgetList<Container>(find.byType(Container)).toList();
        final found = containers.any((c) {
          final dec = c.decoration;
          return dec is BoxDecoration && dec.color == const Color(0xFF5865F2);
        });
        expect(found, isTrue);
      });
    });

    group('타임스탬프 포맷', () {
      testWidgets('shows timeago formatted timestamp in Korean', (tester) async {
        final deliveredAt =
            DateTime.now().subtract(const Duration(minutes: 5));
        final item = _makeItem(deliveredAt: deliveredAt);
        await _pumpBubble(tester, item);

        final expected = timeago.format(deliveredAt, locale: 'ko');
        expect(find.text(expected), findsOneWidget);
      });
    });

    group('탭 콜백', () {
      testWidgets('triggers onTap when tapped', (tester) async {
        bool tapped = false;
        final item = _makeItem();

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: DeliveryBubble(
                  item: item, onTap: () => tapped = true),
            ),
          ),
        );

        await tester.tap(find.byType(GestureDetector));
        expect(tapped, isTrue);
      });

      testWidgets('onTap is not triggered without tap', (tester) async {
        bool tapped = false;
        final item = _makeItem();

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: DeliveryBubble(
                  item: item, onTap: () => tapped = true),
            ),
          ),
        );

        expect(tapped, isFalse);
      });
    });

    testWidgets('content text has maxLines 5 and ellipsis overflow',
        (tester) async {
      const longContent =
          'Line1\nLine2\nLine3\nLine4\nLine5\nLine6 extra overflow content';
      final item = _makeItem(content: longContent);
      await _pumpBubble(tester, item);

      final contentText = tester.widget<Text>(find.text(longContent));
      expect(contentText.maxLines, 5);
      expect(contentText.overflow, TextOverflow.ellipsis);
    });
  });
}
