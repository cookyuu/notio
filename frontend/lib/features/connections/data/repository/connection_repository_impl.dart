import '../../domain/entity/connection_entity.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../../domain/repository/connection_repository.dart';
import '../datasource/connection_remote_datasource.dart';
import '../model/connection_create_request.dart';
import '../model/connection_create_response.dart';
import '../model/connection_oauth_url_request.dart';
import '../model/connection_oauth_url_response.dart';
import '../model/connection_test_response.dart';
import '../model/connection_refresh_response.dart';
import '../model/connection_rotate_key_response.dart';

/// Implementation of ConnectionRepository
///
/// IMPORTANT: This repository does NOT cache API keys or sensitive credentials
/// API keys are only returned in create/rotate responses and should be displayed once
class ConnectionRepositoryImpl implements ConnectionRepository {
  final ConnectionRemoteDataSource _remoteDataSource;

  ConnectionRepositoryImpl({
    required ConnectionRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<ConnectionEntity>> fetchConnections() async {
    try {
      final models = await _remoteDataSource.fetchConnections();
      return models.map((model) => model.toEntity()).toList();
    } catch (e) {
      // Network error handling follows existing pattern
      rethrow;
    }
  }

  @override
  Future<ConnectionEntity> fetchConnectionById(int id) async {
    try {
      final model = await _remoteDataSource.fetchConnectionById(id);
      return model.toEntity();
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<ConnectionCreateResponse> createConnection({
    required ConnectionProvider provider,
    required ConnectionAuthType authType,
    required String displayName,
  }) async {
    try {
      final request = ConnectionCreateRequest.create(
        provider: provider,
        authType: authType,
        displayName: displayName,
      );
      final response = await _remoteDataSource.createConnection(request);

      // IMPORTANT: Do NOT cache the API key
      // It should only be returned to the caller for one-time display
      return response;
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<void> deleteConnection(int id) async {
    try {
      await _remoteDataSource.deleteConnection(id);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<ConnectionTestResponse> testConnection(int id) async {
    try {
      return await _remoteDataSource.testConnection(id);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<ConnectionRefreshResponse> refreshConnection(int id) async {
    try {
      return await _remoteDataSource.refreshConnection(id);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<ConnectionRotateKeyResponse> rotateKey(int id) async {
    try {
      final response = await _remoteDataSource.rotateKey(id);

      // IMPORTANT: Do NOT cache the new API key
      // It should only be returned to the caller for one-time display
      return response;
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<ConnectionOAuthUrlResponse> requestOAuthUrl({
    required ConnectionProvider provider,
    required String displayName,
    String? redirectUri,
  }) async {
    try {
      final request = ConnectionOAuthUrlRequest.create(
        provider: provider,
        displayName: displayName,
        redirectUri: redirectUri,
      );
      return await _remoteDataSource.requestOAuthUrl(request);
    } catch (e) {
      rethrow;
    }
  }
}
