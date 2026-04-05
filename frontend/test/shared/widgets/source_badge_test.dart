import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/shared/widgets/source_badge.dart';

void main() {
  group('SourceBadge Widget Tests', () {
    testWidgets('renders Claude badge correctly', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SourceBadge(source: NotificationSource.claude),
          ),
        ),
      );

      expect(find.text('Claude'), findsOneWidget);
      expect(find.byIcon(Icons.auto_awesome), findsOneWidget);
    });

    testWidgets('renders Slack badge correctly', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SourceBadge(source: NotificationSource.slack),
          ),
        ),
      );

      expect(find.text('Slack'), findsOneWidget);
      expect(find.byIcon(Icons.tag), findsOneWidget);
    });

    testWidgets('renders GitHub badge correctly', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SourceBadge(source: NotificationSource.github),
          ),
        ),
      );

      expect(find.text('GitHub'), findsOneWidget);
      expect(find.byIcon(Icons.code), findsOneWidget);
    });

    testWidgets('renders Gmail badge correctly', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SourceBadge(source: NotificationSource.gmail),
          ),
        ),
      );

      expect(find.text('Gmail'), findsOneWidget);
      expect(find.byIcon(Icons.email), findsOneWidget);
    });

    testWidgets('renders small size badge', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SourceBadge(
              source: NotificationSource.claude,
              size: SourceBadgeSize.small,
            ),
          ),
        ),
      );

      expect(find.byType(SourceBadge), findsOneWidget);
    });

    testWidgets('renders large size badge', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SourceBadge(
              source: NotificationSource.claude,
              size: SourceBadgeSize.large,
            ),
          ),
        ),
      );

      expect(find.byType(SourceBadge), findsOneWidget);
    });
  });
}
