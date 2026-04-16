import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/connections/presentation/providers/connection_providers.dart';
import 'package:notio_app/features/connections/domain/repository/connection_repository.dart';
import 'package:notio_app/features/connections/domain/entity/connection_entity.dart';
import 'package:notio_app/features/connections/domain/entity/connection_provider.dart';
import 'package:notio_app/features/connections/domain/entity/connection_auth_type.dart';
import 'package:notio_app/features/connections/domain/entity/connection_status.dart';
import 'package:notio_app/features/connections/domain/entity/connection_capability.dart';
import 'package:notio_app/features/connections/data/model/connection_model.dart';
import 'package:notio_app/features/connections/data/model/connection_create_response.dart';
import 'package:notio_app/features/connections/data/model/connection_test_response.dart';
import 'package:notio_app/features/connections/data/model/connection_refresh_response.dart';
import 'package:notio_app/features/connections/data/model/connection_rotate_key_response.dart';
import 'package:notio_app/features/connections/data/model/connection_oauth_url_response.dart';

/// Mock implementation of ConnectionRepository for testing
class MockConnectionRepository implements ConnectionRepository {
  List<ConnectionEntity> _mockConnections = [];
  ConnectionEntity? _mockConnection;
  ConnectionCreateResponse? _mockCreateResponse;
  ConnectionTestResponse? _mockTestResponse;
  ConnectionRefreshResponse? _mockRefreshResponse;
  ConnectionRotateKeyResponse? _mockRotateKeyResponse;
  ConnectionOAuthUrlResponse? _mockOAuthUrlResponse;
  Exception? _mockException;

  void setMockConnections(List<ConnectionEntity> connections) {
    _mockConnections = connections;
  }

  void setMockConnection(ConnectionEntity connection) {
    _mockConnection = connection;
  }

  void setMockCreateResponse(ConnectionCreateResponse response) {
    _mockCreateResponse = response;
  }

  void setMockTestResponse(ConnectionTestResponse response) {
    _mockTestResponse = response;
  }

  void setMockRefreshResponse(ConnectionRefreshResponse response) {
    _mockRefreshResponse = response;
  }

  void setMockRotateKeyResponse(ConnectionRotateKeyResponse response) {
    _mockRotateKeyResponse = response;
  }

  void setMockOAuthUrlResponse(ConnectionOAuthUrlResponse response) {
    _mockOAuthUrlResponse = response;
  }

  void setMockException(Exception exception) {
    _mockException = exception;
  }

  @override
  Future<List<ConnectionEntity>> fetchConnections() async {
    if (_mockException != null) throw _mockException!;
    return _mockConnections;
  }

  @override
  Future<ConnectionEntity> fetchConnectionById(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockConnection == null) throw Exception('Not found');
    return _mockConnection!;
  }

  @override
  Future<ConnectionCreateResponse> createConnection({
    required ConnectionProvider provider,
    required ConnectionAuthType authType,
    required String displayName,
  }) async {
    if (_mockException != null) throw _mockException!;
    if (_mockCreateResponse == null) throw Exception('No response');
    return _mockCreateResponse!;
  }

  @override
  Future<void> deleteConnection(int id) async {
    if (_mockException != null) throw _mockException!;
  }

  @override
  Future<ConnectionTestResponse> testConnection(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockTestResponse == null) throw Exception('No response');
    return _mockTestResponse!;
  }

  @override
  Future<ConnectionRefreshResponse> refreshConnection(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockRefreshResponse == null) throw Exception('No response');
    return _mockRefreshResponse!;
  }

  @override
  Future<ConnectionRotateKeyResponse> rotateKey(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockRotateKeyResponse == null) throw Exception('No response');
    return _mockRotateKeyResponse!;
  }

  @override
  Future<ConnectionOAuthUrlResponse> requestOAuthUrl({
    required ConnectionProvider provider,
    required String displayName,
    String? redirectUri,
  }) async {
    if (_mockException != null) throw _mockException!;
    if (_mockOAuthUrlResponse == null) throw Exception('No response');
    return _mockOAuthUrlResponse!;
  }
}

void main() {
  late ProviderContainer container;
  late MockConnectionRepository mockRepository;

  setUp(() {
    mockRepository = MockConnectionRepository();
    container = ProviderContainer(
      overrides: [
        connectionRepositoryProvider.overrideWithValue(mockRepository),
      ],
    );
  });

  tearDown(() {
    container.dispose();
  });

  group('ConnectionsProvider', () {
    test('initially loads connections successfully', () async {
      final mockConnections = [
        ConnectionEntity(
          id: 1,
          provider: ConnectionProvider.claude,
          authType: ConnectionAuthType.apiKey,
          displayName: 'Claude Connection',
          status: ConnectionStatus.active,
          capabilities: [ConnectionCapability.webhookReceive],
          createdAt: DateTime.parse('2024-01-01T00:00:00Z'),
          updatedAt: DateTime.parse('2024-01-01T00:00:00Z'),
        ),
      ];
      mockRepository.setMockConnections(mockConnections);

      final state = await container.read(connectionsProvider.future);

      expect(state, hasLength(1));
      expect(state[0].id, 1);
      expect(state[0].provider, ConnectionProvider.claude);
    });

    test('handles loading state', () {
      mockRepository.setMockConnections([]);

      final state = container.read(connectionsProvider);

      expect(state, isA<AsyncLoading>());
    });

    test('handles error state', () async {
      mockRepository.setMockException(Exception('Network error'));

      // Wait for the provider to load and handle the error
      await container.read(connectionsProvider.future).catchError((error) {
        return <ConnectionEntity>[];
      });

      final state = container.read(connectionsProvider);

      // Check that provider handled the error
      expect(state.hasError, true);
    });

    test('refresh updates connections list', () async {
      final initialConnections = [
        ConnectionEntity(
          id: 1,
          provider: ConnectionProvider.claude,
          authType: ConnectionAuthType.apiKey,
          displayName: 'Old',
          status: ConnectionStatus.active,
          capabilities: [],
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        ),
      ];
      mockRepository.setMockConnections(initialConnections);

      // Initial load
      await container.read(connectionsProvider.future);

      // Update mock data
      final updatedConnections = [
        ConnectionEntity(
          id: 1,
          provider: ConnectionProvider.claude,
          authType: ConnectionAuthType.apiKey,
          displayName: 'New',
          status: ConnectionStatus.active,
          capabilities: [],
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        ),
      ];
      mockRepository.setMockConnections(updatedConnections);

      // Refresh
      await container.read(connectionsProvider.notifier).refresh();

      final state = await container.read(connectionsProvider.future);
      expect(state[0].displayName, 'New');
    });
  });

  group('Filter Providers', () {
    test('provider filter changes value', () {
      container.read(connectionProviderFilterProvider.notifier).state =
          ConnectionProvider.claude;

      final state = container.read(connectionProviderFilterProvider);

      expect(state, ConnectionProvider.claude);
    });

    test('status filter changes value', () {
      container.read(connectionStatusFilterProvider.notifier).state =
          ConnectionStatus.active;

      final state = container.read(connectionStatusFilterProvider);

      expect(state, ConnectionStatus.active);
    });

    test('auth type filter changes value', () {
      container.read(connectionAuthTypeFilterProvider.notifier).state =
          ConnectionAuthType.apiKey;

      final state = container.read(connectionAuthTypeFilterProvider);

      expect(state, ConnectionAuthType.apiKey);
    });
  });

  group('OneTimeApiKeyProvider', () {
    test('initial state has no API key', () {
      final state = container.read(oneTimeApiKeyProvider);

      expect(state.apiKey, isNull);
      expect(state.isVisible, false);
    });

    test('showApiKey sets key and visibility', () {
      container.read(oneTimeApiKeyProvider.notifier).showApiKey('sk-test-key');

      final state = container.read(oneTimeApiKeyProvider);

      expect(state.apiKey, 'sk-test-key');
      expect(state.isVisible, true);
    });

    test('discardApiKey clears key and visibility', () {
      container.read(oneTimeApiKeyProvider.notifier).showApiKey('sk-test-key');
      container.read(oneTimeApiKeyProvider.notifier).discardApiKey();

      final state = container.read(oneTimeApiKeyProvider);

      expect(state.apiKey, isNull);
      expect(state.isVisible, false);
    });

    test('API key cannot be retrieved after discard', () {
      // Show key
      container.read(oneTimeApiKeyProvider.notifier).showApiKey('sk-secret');

      final stateBeforeDiscard = container.read(oneTimeApiKeyProvider);
      expect(stateBeforeDiscard.apiKey, 'sk-secret');

      // Discard key
      container.read(oneTimeApiKeyProvider.notifier).discardApiKey();

      final stateAfterDiscard = container.read(oneTimeApiKeyProvider);
      expect(stateAfterDiscard.apiKey, isNull);
      expect(stateAfterDiscard.isVisible, false);
    });
  });

  group('ConnectionActionsProvider', () {
    test('createConnection shows one-time API key on success', () async {
      final mockConnection = ConnectionModel(
        id: 1,
        provider: 'CLAUDE',
        authType: 'API_KEY',
        displayName: 'New Connection',
        status: 'ACTIVE',
        capabilities: ['WEBHOOK_RECEIVE'],
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      );
      final mockResponse = ConnectionCreateResponse(
        connection: mockConnection,
        apiKey: 'sk-new-api-key',
      );
      mockRepository.setMockCreateResponse(mockResponse);
      mockRepository.setMockConnections([]);

      await container.read(connectionActionsProvider.notifier).createConnection(
            provider: ConnectionProvider.claude,
            authType: ConnectionAuthType.apiKey,
            displayName: 'New Connection',
          );

      final apiKeyState = container.read(oneTimeApiKeyProvider);
      expect(apiKeyState.apiKey, 'sk-new-api-key');
      expect(apiKeyState.isVisible, true);
    });

    test('rotateKey shows one-time API key on success', () async {
      final mockConnection = ConnectionModel(
        id: 1,
        provider: 'CLAUDE',
        authType: 'API_KEY',
        displayName: 'Test',
        status: 'ACTIVE',
        capabilities: [],
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T10:00:00Z',
      );
      final mockResponse = ConnectionRotateKeyResponse(
        connection: mockConnection,
        apiKey: 'sk-rotated-key',
      );
      mockRepository.setMockRotateKeyResponse(mockResponse);
      mockRepository.setMockConnections([]);

      await container.read(connectionActionsProvider.notifier).rotateKey(1);

      final apiKeyState = container.read(oneTimeApiKeyProvider);
      expect(apiKeyState.apiKey, 'sk-rotated-key');
      expect(apiKeyState.isVisible, true);
    });

    test('deleteConnection refreshes list on success', () async {
      final initialConnections = [
        ConnectionEntity(
          id: 1,
          provider: ConnectionProvider.claude,
          authType: ConnectionAuthType.apiKey,
          displayName: 'To Delete',
          status: ConnectionStatus.active,
          capabilities: [],
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        ),
      ];
      mockRepository.setMockConnections(initialConnections);

      // Load initial state
      await container.read(connectionsProvider.future);

      // Set empty list (simulating deletion)
      mockRepository.setMockConnections([]);

      // Delete connection
      await container.read(connectionActionsProvider.notifier).deleteConnection(1);

      final connections = await container.read(connectionsProvider.future);
      expect(connections, isEmpty);
    });

    test('testConnection sets success message', () async {
      final mockResponse = ConnectionTestResponse(
        success: true,
        message: 'Test successful',
        testedAt: '2024-01-15T10:00:00Z',
      );
      mockRepository.setMockTestResponse(mockResponse);

      await container.read(connectionActionsProvider.notifier).testConnection(1);

      final state = container.read(connectionActionsProvider);
      expect(state.successMessage, 'Test successful');
      expect(state.errorMessage, isNull);
    });

    test('testConnection sets error message on failure', () async {
      final mockResponse = ConnectionTestResponse(
        success: false,
        message: 'Test failed',
        testedAt: '2024-01-15T10:00:00Z',
      );
      mockRepository.setMockTestResponse(mockResponse);

      await container.read(connectionActionsProvider.notifier).testConnection(1);

      final state = container.read(connectionActionsProvider);
      expect(state.errorMessage, 'Test failed');
      expect(state.successMessage, isNull);
    });

    test('refreshConnection updates list on success', () async {
      final mockResponse = ConnectionRefreshResponse(
        id: 1,
        status: 'ACTIVE',
        refreshedAt: '2024-01-15T10:00:00Z',
      );
      mockRepository.setMockRefreshResponse(mockResponse);
      mockRepository.setMockConnections([]);

      await container.read(connectionActionsProvider.notifier).refreshConnection(1);

      final state = container.read(connectionActionsProvider);
      expect(state.successMessage, 'Token refreshed successfully');
    });
  });

  group('OAuthStateProvider', () {
    test('initial state is not in progress', () {
      final state = container.read(oauthStateProvider);

      expect(state.isInProgress, false);
      expect(state.authorizationUrl, isNull);
      expect(state.errorMessage, isNull);
    });

    test('startOAuth sets authorization URL on success', () async {
      final mockResponse = ConnectionOAuthUrlResponse(
        authorizationUrl: 'https://oauth.example.com/authorize',
        stateExpiresIn: 600,
      );
      mockRepository.setMockOAuthUrlResponse(mockResponse);

      await container.read(oauthStateProvider.notifier).startOAuth(
            provider: ConnectionProvider.slack,
            displayName: 'Slack Connection',
          );

      final state = container.read(oauthStateProvider);
      expect(state.isInProgress, false);
      expect(state.authorizationUrl, 'https://oauth.example.com/authorize');
      expect(state.errorMessage, isNull);
    });

    test('startOAuth sets error message on failure', () async {
      mockRepository.setMockException(Exception('OAuth request failed'));

      await container.read(oauthStateProvider.notifier).startOAuth(
            provider: ConnectionProvider.github,
            displayName: 'GitHub Connection',
          );

      final state = container.read(oauthStateProvider);
      expect(state.isInProgress, false);
      expect(state.errorMessage, isNotNull);
    });

    test('reset clears OAuth state', () async {
      final mockResponse = ConnectionOAuthUrlResponse(
        authorizationUrl: 'https://oauth.example.com',
        stateExpiresIn: 600,
      );
      mockRepository.setMockOAuthUrlResponse(mockResponse);

      await container.read(oauthStateProvider.notifier).startOAuth(
            provider: ConnectionProvider.slack,
            displayName: 'Test',
          );

      container.read(oauthStateProvider.notifier).reset();

      final state = container.read(oauthStateProvider);
      expect(state.isInProgress, false);
      expect(state.authorizationUrl, isNull);
      expect(state.errorMessage, isNull);
    });
  });
}
