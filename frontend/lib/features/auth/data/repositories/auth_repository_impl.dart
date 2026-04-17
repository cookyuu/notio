import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:notio_app/features/auth/data/datasources/auth_api_client.dart';
import 'package:notio_app/features/auth/data/models/find_id_request.dart';
import 'package:notio_app/features/auth/data/models/find_id_response.dart';
import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/oauth_exchange_request.dart';
import 'package:notio_app/features/auth/data/models/oauth_exchange_response.dart';
import 'package:notio_app/features/auth/data/models/oauth_start_request.dart';
import 'package:notio_app/features/auth/data/models/oauth_start_response.dart';
import 'package:notio_app/features/auth/data/models/password_reset_confirm_request.dart';
import 'package:notio_app/features/auth/data/models/password_reset_confirm_response.dart';
import 'package:notio_app/features/auth/data/models/password_reset_request_request.dart';
import 'package:notio_app/features/auth/data/models/password_reset_request_response.dart';
import 'package:notio_app/features/auth/data/models/signup_request.dart';
import 'package:notio_app/features/auth/data/models/signup_response.dart';
import 'package:notio_app/features/auth/domain/auth_token_policy.dart';
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
      final response = await _apiClient.login(request);
      await _storeTokens(response);
      return response;
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
      if (AuthTokenPolicy.isMockToken(refreshToken)) {
        await _clearStoredAuth();
        throw Exception('Stored mock refresh token is not valid');
      }

      final response = await _apiClient.refreshToken({
        'refreshToken': refreshToken,
      });

      await _storage.write(key: _accessTokenKey, value: response.accessToken);
      await _storage.write(key: _refreshTokenKey, value: response.refreshToken);
      await _storage.write(
        key: _expiresAtKey,
        value: response.expiresAt.toIso8601String(),
      );
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<void> logout() async {
    try {
      await _apiClient.logout();

      // Clear all stored data
      await _clearStoredAuth();
    } catch (e) {
      // Always clear local storage even if API call fails
      await _clearStoredAuth();
      rethrow;
    }
  }

  @override
  Future<bool> isLoggedIn() async {
    final accessToken = await getAccessToken();
    if (accessToken == null) return false;
    if (AuthTokenPolicy.isMockToken(accessToken)) {
      await _clearStoredAuth();
      return false;
    }

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

  Future<void> _clearStoredAuth() async {
    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _refreshTokenKey);
    await _storage.delete(key: _userEmailKey);
    await _storage.delete(key: _expiresAtKey);
  }

  @override
  Future<SignupResponse> signup(SignupRequest request) async {
    try {
      return await _apiClient.signup(request);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<FindIdResponse> findId(FindIdRequest request) async {
    try {
      return await _apiClient.findId(request);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<PasswordResetRequestResponse> requestPasswordReset(
      PasswordResetRequestRequest request) async {
    try {
      return await _apiClient.requestPasswordReset(request);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<PasswordResetConfirmResponse> confirmPasswordReset(
      PasswordResetConfirmRequest request) async {
    try {
      return await _apiClient.confirmPasswordReset(request);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<OAuthStartResponse> startSocialLogin(OAuthStartRequest request) async {
    try {
      return await _apiClient.startSocialLogin(request);
    } catch (e) {
      rethrow;
    }
  }

  @override
  Future<OAuthExchangeResponse> exchangeSocialLogin(
      OAuthExchangeRequest request) async {
    try {
      return await _apiClient.exchangeSocialLogin(request);
    } catch (e) {
      rethrow;
    }
  }

}
