import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:notio_app/features/auth/data/datasources/auth_api_client.dart';
import 'package:notio_app/features/auth/data/datasources/auth_mock_data.dart';
import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthApiClient _apiClient;
  final FlutterSecureStorage _storage;

  static const String _accessTokenKey = 'auth_access_token';
  static const String _refreshTokenKey = 'auth_refresh_token';
  static const String _userEmailKey = 'auth_user_email';
  static const String _expiresAtKey = 'auth_expires_at';

  AuthRepositoryImpl({
    required AuthApiClient apiClient,
    required FlutterSecureStorage storage,
  })  : _apiClient = apiClient,
        _storage = storage;

  @override
  Future<LoginResponse> login(LoginRequest request) async {
    try {
      // Check if we should use mock data
      if (AuthMockData.useMockData) {
        // Simulate network delay
        await Future.delayed(const Duration(seconds: 1));

        // Validate credentials
        if (!AuthMockData.validateMockCredentials(
            request.email, request.password)) {
          throw Exception('Invalid credentials');
        }

        // Return mock data
        final response = AuthMockData.mockLoginResponse(request.email);

        // Store tokens
        await _storeTokens(response);

        return response;
      } else {
        // Call real API
        final response = await _apiClient.login(request);
        await _storeTokens(response);
        return response;
      }
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<void> refreshToken() async {
    try {
      final refreshToken = await getRefreshToken();
      if (refreshToken == null) {
        throw Exception('No refresh token found');
      }

      if (AuthMockData.useMockData) {
        // Simulate network delay
        await Future.delayed(const Duration(milliseconds: 500));

        final response = AuthMockData.mockRefreshTokenResponse();

        // Update tokens
        await _storage.write(key: _accessTokenKey, value: response.accessToken);
        await _storage.write(
            key: _refreshTokenKey, value: response.refreshToken);
        await _storage.write(
            key: _expiresAtKey, value: response.expiresAt.toIso8601String());
      } else {
        final response = await _apiClient.refreshToken({
          'refreshToken': refreshToken,
        });

        await _storage.write(key: _accessTokenKey, value: response.accessToken);
        await _storage.write(
            key: _refreshTokenKey, value: response.refreshToken);
        await _storage.write(
            key: _expiresAtKey, value: response.expiresAt.toIso8601String());
      }
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<void> logout() async {
    try {
      if (!AuthMockData.useMockData) {
        await _apiClient.logout();
      }

      // Clear all stored data
      await _storage.delete(key: _accessTokenKey);
      await _storage.delete(key: _refreshTokenKey);
      await _storage.delete(key: _userEmailKey);
      await _storage.delete(key: _expiresAtKey);
    } catch (e) {
      // Always clear local storage even if API call fails
      await _storage.delete(key: _accessTokenKey);
      await _storage.delete(key: _refreshTokenKey);
      await _storage.delete(key: _userEmailKey);
      await _storage.delete(key: _expiresAtKey);
      rethrow;
    }
  }

  @override
  Future<bool> isLoggedIn() async {
    final accessToken = await getAccessToken();
    if (accessToken == null) return false;

    // Check if token is expired
    final expiresAtStr = await _storage.read(key: _expiresAtKey);
    if (expiresAtStr == null) return false;

    final expiresAt = DateTime.parse(expiresAtStr);
    if (DateTime.now().isAfter(expiresAt)) {
      // Token expired, try to refresh
      try {
        await refreshToken();
        return true;
      } catch (e) {
        return false;
      }
    }

    return true;
  }

  @override
  Future<String?> getAccessToken() async {
    return await _storage.read(key: _accessTokenKey);
  }

  @override
  Future<String?> getRefreshToken() async {
    return await _storage.read(key: _refreshTokenKey);
  }

  @override
  Future<String?> getUserEmail() async {
    return await _storage.read(key: _userEmailKey);
  }

  Future<void> _storeTokens(LoginResponse response) async {
    await _storage.write(key: _accessTokenKey, value: response.accessToken);
    await _storage.write(key: _refreshTokenKey, value: response.refreshToken);
    await _storage.write(key: _userEmailKey, value: response.email);
    await _storage.write(
        key: _expiresAtKey, value: response.expiresAt.toIso8601String());
  }
}
