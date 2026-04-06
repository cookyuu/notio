import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';

/// Mock data provider for auth when backend is not available
class AuthMockData {
  AuthMockData._();

  static const bool useMockData = true; // Set to false when backend is ready

  /// Mock login response
  static LoginResponse mockLoginResponse(String email) {
    return LoginResponse(
      userId: 'mock-user-${email.hashCode}',
      email: email,
      accessToken: 'mock-access-token-${DateTime.now().millisecondsSinceEpoch}',
      refreshToken:
          'mock-refresh-token-${DateTime.now().millisecondsSinceEpoch}',
      expiresIn: 3600, // 1 hour
    );
  }

  /// Mock refresh token response
  static RefreshTokenResponse mockRefreshTokenResponse() {
    return RefreshTokenResponse(
      accessToken:
          'mock-access-token-refreshed-${DateTime.now().millisecondsSinceEpoch}',
      refreshToken:
          'mock-refresh-token-refreshed-${DateTime.now().millisecondsSinceEpoch}',
      expiresIn: 3600, // 1 hour
    );
  }

  /// Validate mock credentials
  static bool validateMockCredentials(String email, String password) {
    // For mock: accept any email with password 'password123'
    // In production, this will be handled by backend
    return password == 'password123';
  }
}
