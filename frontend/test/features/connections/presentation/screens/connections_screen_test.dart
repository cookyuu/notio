import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/connections/data/model/connection_create_response.dart';
import 'package:notio_app/features/connections/data/model/connection_oauth_url_response.dart';
import 'package:notio_app/features/connections/data/model/connection_refresh_response.dart';
import 'package:notio_app/features/connections/data/model/connection_rotate_key_response.dart';
import 'package:notio_app/features/connections/data/model/connection_test_response.dart';
import 'package:notio_app/features/connections/domain/entity/connection_auth_type.dart';
import 'package:notio_app/features/connections/domain/entity/connection_capability.dart';
import 'package:notio_app/features/connections/domain/entity/connection_entity.dart';
import 'package:notio_app/features/connections/domain/entity/connection_provider.dart';
import 'package:notio_app/features/connections/domain/entity/connection_status.dart';
import 'package:notio_app/features/connections/domain/repository/connection_repository.dart';
import 'package:notio_app/features/connections/presentation/providers/connection_providers.dart';
import 'package:notio_app/features/connections/presentation/screens/connections_screen.dart';

class _FakeConnectionRepository implements ConnectionRepository {
  _FakeConnectionRepository(this.connections);

  final List<ConnectionEntity> connections;

  @override
  Future<List<ConnectionEntity>> fetchConnections() async => connections;

  @override
  Future<ConnectionEntity> fetchConnectionById(int id) async => connections.first;

  @override
  Future<ConnectionCreateResponse> createConnection({
    required ConnectionProvider provider,
    required ConnectionAuthType authType,
    required String displayName,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteConnection(int id) async {}

  @override
  Future<ConnectionOAuthUrlResponse> requestOAuthUrl({
    required ConnectionProvider provider,
    required String displayName,
    String? redirectUri,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<ConnectionRefreshResponse> refreshConnection(int id) async {
    throw UnimplementedError();
  }

  @override
  Future<ConnectionRotateKeyResponse> rotateKey(int id) async {
    throw UnimplementedError();
  }

  @override
  Future<ConnectionTestResponse> testConnection(int id) async {
    throw UnimplementedError();
  }
}

void main() {
  group('ConnectionsScreen', () {
    final sampleConnections = [
      ConnectionEntity(
        id: 1,
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Claude',
        status: ConnectionStatus.active,
        capabilities: const [ConnectionCapability.webhookReceive],
        createdAt: DateTime(2024, 1, 1),
        updatedAt: DateTime(2024, 1, 1),
      ),
    ];

    Future<void> pumpConnectionsScreen(WidgetTester tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            connectionRepositoryProvider.overrideWith(
              (ref) => _FakeConnectionRepository(sampleConnections),
            ),
          ],
          child: const MaterialApp(
            home: ConnectionsScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('displays screen title', (tester) async {
      await pumpConnectionsScreen(tester);

      expect(find.text('연동 관리'), findsOneWidget);
    });

    testWidgets('has add connection button', (tester) async {
      await pumpConnectionsScreen(tester);

      expect(find.byIcon(Icons.add), findsOneWidget);
    });

    testWidgets('shows filter options', (tester) async {
      await pumpConnectionsScreen(tester);

      expect(find.text('Filters'), findsOneWidget);
      expect(find.text('All'), findsWidgets);
      expect(find.text('Active'), findsWidgets);
      expect(find.text('Needs Action'), findsOneWidget);
    });

    testWidgets('supports pull to refresh', (tester) async {
      await pumpConnectionsScreen(tester);

      expect(find.byType(RefreshIndicator), findsOneWidget);
    });
  });
}
