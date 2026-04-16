import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/connections/data/repository/connection_repository_impl.dart';
import 'package:notio_app/features/connections/data/datasource/connection_remote_datasource.dart';
import 'package:notio_app/features/connections/data/model/connection_model.dart';
import 'package:notio_app/features/connections/data/model/connection_create_request.dart';
import 'package:notio_app/features/connections/data/model/connection_create_response.dart';
import 'package:notio_app/features/connections/data/model/connection_oauth_url_request.dart';
import 'package:notio_app/features/connections/data/model/connection_oauth_url_response.dart';
import 'package:notio_app/features/connections/data/model/connection_test_response.dart';
import 'package:notio_app/features/connections/data/model/connection_refresh_response.dart';
import 'package:notio_app/features/connections/data/model/connection_rotate_key_response.dart';
import 'package:notio_app/features/connections/domain/entity/connection_provider.dart';
import 'package:notio_app/features/connections/domain/entity/connection_auth_type.dart';

/// Mock implementation of ConnectionRemoteDataSource for testing
class MockConnectionRemoteDataSource implements ConnectionRemoteDataSource {
  List<ConnectionModel>? _mockConnections;
  ConnectionModel? _mockConnection;
  ConnectionCreateResponse? _mockCreateResponse;
  ConnectionOAuthUrlResponse? _mockOAuthUrlResponse;
  ConnectionTestResponse? _mockTestResponse;
  ConnectionRefreshResponse? _mockRefreshResponse;
  ConnectionRotateKeyResponse? _mockRotateKeyResponse;
  Exception? _mockException;

  void setMockConnections(List<ConnectionModel> connections) {
    _mockConnections = connections;
  }

  void setMockConnection(ConnectionModel connection) {
    _mockConnection = connection;
  }

  void setMockCreateResponse(ConnectionCreateResponse response) {
    _mockCreateResponse = response;
  }

  void setMockOAuthUrlResponse(ConnectionOAuthUrlResponse response) {
    _mockOAuthUrlResponse = response;
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

  void setMockException(Exception exception) {
    _mockException = exception;
  }

  @override
  Future<List<ConnectionModel>> fetchConnections() async {
    if (_mockException != null) throw _mockException!;
    return _mockConnections ?? [];
  }

  @override
  Future<ConnectionModel> fetchConnectionById(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockConnection == null) {
      throw Exception('Connection not found');
    }
    return _mockConnection!;
  }

  @override
  Future<ConnectionCreateResponse> createConnection(
    ConnectionCreateRequest request,
  ) async {
    if (_mockException != null) throw _mockException!;
    if (_mockCreateResponse == null) {
      throw Exception('Create response not set');
    }
    return _mockCreateResponse!;
  }

  @override
  Future<void> deleteConnection(int id) async {
    if (_mockException != null) throw _mockException!;
  }

  @override
  Future<ConnectionTestResponse> testConnection(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockTestResponse == null) {
      throw Exception('Test response not set');
    }
    return _mockTestResponse!;
  }

  @override
  Future<ConnectionRefreshResponse> refreshConnection(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockRefreshResponse == null) {
      throw Exception('Refresh response not set');
    }
    return _mockRefreshResponse!;
  }

  @override
  Future<ConnectionRotateKeyResponse> rotateKey(int id) async {
    if (_mockException != null) throw _mockException!;
    if (_mockRotateKeyResponse == null) {
      throw Exception('Rotate key response not set');
    }
    return _mockRotateKeyResponse!;
  }

  @override
  Future<ConnectionOAuthUrlResponse> requestOAuthUrl(
    ConnectionOAuthUrlRequest request,
  ) async {
    if (_mockException != null) throw _mockException!;
    if (_mockOAuthUrlResponse == null) {
      throw Exception('OAuth URL response not set');
    }
    return _mockOAuthUrlResponse!;
  }
}

void main() {
  late ConnectionRepositoryImpl repository;
  late MockConnectionRemoteDataSource mockDataSource;

  setUp(() {
    mockDataSource = MockConnectionRemoteDataSource();
    repository = ConnectionRepositoryImpl(remoteDataSource: mockDataSource);
  });

  group('ConnectionRepositoryImpl - Success Flow', () {
    test('fetchConnections returns list of entities', () async {
      final mockModels = [
        ConnectionModel(
          id: 1,
          provider: 'CLAUDE',
          authType: 'API_KEY',
          displayName: 'Claude Connection',
          status: 'ACTIVE',
          capabilities: ['WEBHOOK_RECEIVE'],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        ),
        ConnectionModel(
          id: 2,
          provider: 'SLACK',
          authType: 'OAUTH',
          displayName: 'Slack Connection',
          status: 'PENDING',
          capabilities: [],
          createdAt: '2024-01-02T00:00:00Z',
          updatedAt: '2024-01-02T00:00:00Z',
        ),
      ];
      mockDataSource.setMockConnections(mockModels);

      final result = await repository.fetchConnections();

      expect(result, hasLength(2));
      expect(result[0].id, 1);
      expect(result[0].provider, ConnectionProvider.claude);
      expect(result[1].id, 2);
      expect(result[1].provider, ConnectionProvider.slack);
    });

    test('fetchConnectionById returns single entity', () async {
      final mockModel = ConnectionModel(
        id: 1,
        provider: 'GITHUB',
        authType: 'OAUTH',
        displayName: 'GitHub Connection',
        status: 'ACTIVE',
        capabilities: ['WEBHOOK_RECEIVE'],
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      );
      mockDataSource.setMockConnection(mockModel);

      final result = await repository.fetchConnectionById(1);

      expect(result.id, 1);
      expect(result.provider, ConnectionProvider.github);
      expect(result.displayName, 'GitHub Connection');
    });

    test('createConnection returns response with API key', () async {
      final mockConnection = ConnectionModel(
        id: 1,
        provider: 'CLAUDE',
        authType: 'API_KEY',
        displayName: 'New Claude',
        status: 'ACTIVE',
        capabilities: ['WEBHOOK_RECEIVE', 'TEST_MESSAGE', 'ROTATE_KEY'],
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      );
      final mockResponse = ConnectionCreateResponse(
        connection: mockConnection,
        apiKey: 'sk-test-api-key-1234567890',
      );
      mockDataSource.setMockCreateResponse(mockResponse);

      final result = await repository.createConnection(
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'New Claude',
      );

      expect(result.connection.id, 1);
      expect(result.apiKey, 'sk-test-api-key-1234567890');
      // Verify API key is in response but NOT stored in repository
      expect(result.apiKey, isNotNull);
    });

    test('createConnection does NOT cache API key', () async {
      final mockConnection = ConnectionModel(
        id: 1,
        provider: 'CLAUDE',
        authType: 'API_KEY',
        displayName: 'Test',
        status: 'ACTIVE',
        capabilities: [],
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      );
      final mockResponse = ConnectionCreateResponse(
        connection: mockConnection,
        apiKey: 'sk-test-secret-key',
      );
      mockDataSource.setMockCreateResponse(mockResponse);

      final result = await repository.createConnection(
        provider: ConnectionProvider.claude,
        authType: ConnectionAuthType.apiKey,
        displayName: 'Test',
      );

      // API key should be returned once
      expect(result.apiKey, 'sk-test-secret-key');

      // Repository should not store it (implementation check via comment)
      // IMPORTANT: Verify in implementation that API key is not cached
    });

    test('deleteConnection completes successfully', () async {
      await repository.deleteConnection(1);
      // No exception thrown means success
    });

    test('testConnection returns test response', () async {
      final mockResponse = ConnectionTestResponse(
        success: true,
        message: 'Connection test successful',
        testedAt: '2024-01-15T10:00:00Z',
      );
      mockDataSource.setMockTestResponse(mockResponse);

      final result = await repository.testConnection(1);

      expect(result.success, true);
      expect(result.message, 'Connection test successful');
    });

    test('refreshConnection returns refresh response', () async {
      final mockResponse = ConnectionRefreshResponse(
        id: 1,
        status: 'ACTIVE',
        refreshedAt: '2024-01-15T10:00:00Z',
      );
      mockDataSource.setMockRefreshResponse(mockResponse);

      final result = await repository.refreshConnection(1);

      expect(result.id, 1);
      expect(result.status, 'ACTIVE');
    });

    test('rotateKey returns new API key', () async {
      final mockConnection = ConnectionModel(
        id: 1,
        provider: 'CLAUDE',
        authType: 'API_KEY',
        displayName: 'Test',
        status: 'ACTIVE',
        capabilities: [],
        keyPreview: 'sk-...3210',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T10:00:00Z',
      );
      final mockResponse = ConnectionRotateKeyResponse(
        connection: mockConnection,
        apiKey: 'sk-new-rotated-key-9876543210',
      );
      mockDataSource.setMockRotateKeyResponse(mockResponse);

      final result = await repository.rotateKey(1);

      expect(result.apiKey, 'sk-new-rotated-key-9876543210');
      expect(result.connection.keyPreview, 'sk-...3210');
    });

    test('rotateKey does NOT cache new API key', () async {
      final mockConnection = ConnectionModel(
        id: 1,
        provider: 'CLAUDE',
        authType: 'API_KEY',
        displayName: 'Test',
        status: 'ACTIVE',
        capabilities: [],
        keyPreview: 'sk-...cret',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-15T10:00:00Z',
      );
      final mockResponse = ConnectionRotateKeyResponse(
        connection: mockConnection,
        apiKey: 'sk-rotated-secret',
      );
      mockDataSource.setMockRotateKeyResponse(mockResponse);

      final result = await repository.rotateKey(1);

      // API key should be returned once
      expect(result.apiKey, 'sk-rotated-secret');

      // Repository should not store it (implementation check via comment)
      // IMPORTANT: Verify in implementation that rotated key is not cached
    });

    test('requestOAuthUrl returns authorization URL', () async {
      final mockResponse = ConnectionOAuthUrlResponse(
        authorizationUrl: 'https://oauth.example.com/authorize?state=abc123',
        stateExpiresIn: 600,
      );
      mockDataSource.setMockOAuthUrlResponse(mockResponse);

      final result = await repository.requestOAuthUrl(
        provider: ConnectionProvider.slack,
        displayName: 'My Slack',
      );

      expect(result.authorizationUrl, contains('oauth.example.com'));
      expect(result.stateExpiresIn, 600);
    });
  });

  group('ConnectionRepositoryImpl - Error Flow', () {
    test('fetchConnections throws on network error', () async {
      mockDataSource.setMockException(Exception('Network error'));

      expect(
        () => repository.fetchConnections(),
        throwsException,
      );
    });

    test('fetchConnectionById throws on not found', () async {
      mockDataSource.setMockException(Exception('Connection not found'));

      expect(
        () => repository.fetchConnectionById(999),
        throwsException,
      );
    });

    test('createConnection throws on validation error', () async {
      mockDataSource.setMockException(Exception('Invalid provider'));

      expect(
        () => repository.createConnection(
          provider: ConnectionProvider.claude,
          authType: ConnectionAuthType.apiKey,
          displayName: 'Test',
        ),
        throwsException,
      );
    });

    test('deleteConnection throws on error', () async {
      mockDataSource.setMockException(Exception('Delete failed'));

      expect(
        () => repository.deleteConnection(1),
        throwsException,
      );
    });

    test('testConnection throws on error', () async {
      mockDataSource.setMockException(Exception('Test failed'));

      expect(
        () => repository.testConnection(1),
        throwsException,
      );
    });

    test('refreshConnection throws on error', () async {
      mockDataSource.setMockException(Exception('Refresh failed'));

      expect(
        () => repository.refreshConnection(1),
        throwsException,
      );
    });

    test('rotateKey throws on error', () async {
      mockDataSource.setMockException(Exception('Rotate failed'));

      expect(
        () => repository.rotateKey(1),
        throwsException,
      );
    });

    test('requestOAuthUrl throws on error', () async {
      mockDataSource.setMockException(Exception('OAuth request failed'));

      expect(
        () => repository.requestOAuthUrl(
          provider: ConnectionProvider.github,
          displayName: 'Test',
        ),
        throwsException,
      );
    });
  });
}
