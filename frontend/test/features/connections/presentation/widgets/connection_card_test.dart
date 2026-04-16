import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/connections/presentation/widgets/connection_card.dart';
import 'package:notio_app/features/connections/domain/entity/connection_entity.dart';
import 'package:notio_app/features/connections/domain/entity/connection_provider.dart';
import 'package:notio_app/features/connections/domain/entity/connection_auth_type.dart';
import 'package:notio_app/features/connections/domain/entity/connection_status.dart';

void main() {
  group('ConnectionCard', () {
    testWidgets('displays connection display name', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'My Claude Connection',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      expect(find.text('My Claude Connection'), findsOneWidget);
    });

    testWidgets('displays account label when present', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.slack,
        authType: ConnectionAuthType.oauth,
        displayName: 'Slack Connection',
        accountLabel: 'user@example.com',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      expect(find.text('user@example.com'), findsOneWidget);
    });

    testWidgets('displays auth type label', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.github,
        authType: ConnectionAuthType.oauth,
        displayName: 'GitHub',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      expect(find.text('OAuth'), findsOneWidget);
    });

    testWidgets('displays API Key auth type', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Claude',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      expect(find.text('API Key'), findsOneWidget);
    });

    testWidgets('shows status badge', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Test',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      // ConnectionStatusBadge should display 'Active'
      expect(find.text('Active'), findsOneWidget);
    });

    testWidgets('shows provider icon', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Claude',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      // Icon should be present (ConnectionProviderIcon.buildIcon)
      expect(find.byType(Icon), findsAtLeastNWidgets(1));
    });

    testWidgets('calls onTap when tapped', (tester) async {
      var tapped = false;
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Test',
        status: ConnectionStatus.active,
        capabilities: [],
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(
              connection: connection,
              onTap: () => tapped = true,
            ),
          ),
        ),
      );

      await tester.tap(find.byType(ConnectionCard));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('displays key preview when present', (tester) async {
      final connection = ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Claude',
        status: ConnectionStatus.active,
        capabilities: [],
        keyPreview: 'sk-...1234',
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ConnectionCard(connection: connection),
          ),
        ),
      );

      expect(find.text('sk-...1234'), findsOneWidget);
    });
  });
}
