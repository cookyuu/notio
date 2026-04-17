import 'package:flutter/material.dart';
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
    expect(find.text('#design'), findsOneWidget);
    expect(find.text('min'), findsOneWidget);
  });
}
