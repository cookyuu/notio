import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';

/// Auth repository interface
abstract class AuthRepository {
  /// Login with email and password
  Future<LoginResponse> login(LoginRequest request);

  /// Refresh access token
  Future<void> refreshToken();

  /// Logout
  Future<void> logout();

  /// Check if user is logged in
  Future<bool> isLoggedIn();

  /// Get stored access token
  Future<String?> getAccessToken();

  /// Get stored refresh token
  Future<String?> getRefreshToken();

  /// Get user email
  Future<String?> getUserEmail();
}
