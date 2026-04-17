import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/features/notification/presentation/widgets/notification_detail_modal.dart';

void main() {
  testWidgets('NotificationDetailModal renders detail response data', (
    tester,
  ) async {
    final notification = NotificationDetailEntity(
      id: 7,
      source: NotificationSource.slack,
      title: 'Design review reminder',
      body: 'Please review the updated notification detail flow before noon.',
      priority: NotificationPriority.medium,
      isRead: true,
      createdAt: DateTime(2026, 4, 17),
      updatedAt: DateTime(2026, 4, 17),
      externalUrl: 'https://slack.com/app_redirect?channel=design',
      metadata: {
        'channel': '#design',
        'requester': 'min',
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: NotificationDetailModal(
            notification: notification,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Design review reminder'), findsOneWidget);
    expect(
      find.text(
          'Please review the updated notification detail flow before noon.'),
      findsOneWidget,
    );
    expect(find.text('외부 링크'), findsOneWidget);
    expect(find.text('추가 정보'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('외부 링크 열기'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.pumpAndSettle();
    expect(find.text('외부 링크 열기'), findsOneWidget);
    expect(find.text('복사'), findsOneWidget);
    expect(find.text('channel:'), findsOneWidget);
    expect(find.text('#design'), findsOneWidget);
    expect(find.text('requester:'), findsOneWidget);
    expect(find.text('min'), findsOneWidget);
  });

  testWidgets('copy action includes body, external url, and metadata', (
    tester,
  ) async {
    String? copiedText;

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, (call) async {
      if (call.method == 'Clipboard.setData') {
        copiedText = (call.arguments as Map<dynamic, dynamic>)['text'] as String?;
      }
      return null;
    });
    addTearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(SystemChannels.platform, null);
    });

    final notification = NotificationDetailEntity(
      id: 11,
      source: NotificationSource.github,
      title: 'PR review requested',
      body: 'Please review the notification detail modal patch.',
      priority: NotificationPriority.high,
      isRead: true,
      createdAt: DateTime(2026, 4, 17),
      updatedAt: DateTime(2026, 4, 17),
      externalUrl: 'https://github.com/notio/notio/pull/11',
      metadata: const {
        'repository': 'notio/notio',
        'reviewer': 'cookyuu',
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: NotificationDetailModal(
            notification: notification,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('복사'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('복사'));
    await tester.pump();

    expect(copiedText, contains('PR review requested'));
    expect(
      copiedText,
      contains('Please review the notification detail modal patch.'),
    );
    expect(
      copiedText,
      contains('https://github.com/notio/notio/pull/11'),
    );
    expect(copiedText, contains('repository: notio/notio'));
    expect(copiedText, contains('reviewer: cookyuu'));
    expect(find.text('상세 내용을 클립보드에 복사했습니다'), findsOneWidget);
  });
}
