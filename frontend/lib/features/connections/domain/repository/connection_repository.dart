import '../entity/connection_entity.dart';
import '../entity/connection_provider.dart';
import '../entity/connection_auth_type.dart';
import '../../data/model/connection_create_response.dart';
import '../../data/model/connection_oauth_url_response.dart';
import '../../data/model/connection_test_response.dart';
import '../../data/model/connection_refresh_response.dart';
import '../../data/model/connection_rotate_key_response.dart';

/// Abstract repository for connections
abstract class ConnectionRepository {
  /// Fetch all connections
  ///
  /// Returns a list of all user connections from various providers
  Future<List<ConnectionEntity>> fetchConnections();

  /// Fetch connection by ID
  ///
  /// Returns a single connection entity
  /// Throws exception if connection is not found
  Future<ConnectionEntity> fetchConnectionById(int id);

  /// Create new connection (API Key)
  ///
  /// Creates a new connection with API Key authentication
  /// Returns the created connection and one-time API key (if applicable)
  /// IMPORTANT: The API key in the response should ONLY be displayed once
  Future<ConnectionCreateResponse> createConnection({
    required ConnectionProvider provider,
    required ConnectionAuthType authType,
    required String displayName,
  });

  /// Delete connection
  ///
  /// Removes a connection and revokes associated credentials
  Future<void> deleteConnection(int id);

  /// Test connection
  ///
  /// Verifies if the connection is working properly
  /// Returns test result with success status and optional message
  Future<ConnectionTestResponse> testConnection(int id);

  /// Refresh OAuth token
  ///
  /// Refreshes the OAuth access token for OAuth-based connections
  /// Returns the refresh result with updated status
  Future<ConnectionRefreshResponse> refreshConnection(int id);

  /// Rotate API Key
  ///
  /// Generates a new API key and invalidates the old one
  /// Returns the new API key (one-time display only)
  /// IMPORTANT: The API key in the response should ONLY be displayed once
  Future<ConnectionRotateKeyResponse> rotateKey(int id);

  /// Request OAuth authorization URL
  ///
  /// Requests the OAuth authorization URL from backend
  /// Returns the URL to redirect user for OAuth flow
  Future<ConnectionOAuthUrlResponse> requestOAuthUrl({
    required ConnectionProvider provider,
    required String displayName,
    String? redirectUri,
  });
}
