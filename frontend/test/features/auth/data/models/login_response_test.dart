import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';

void main() {
  group('LoginResponse', () {
    test('parses snake_case auth response', () {
      final response = LoginResponse.fromJson({
        'user_id': 1,
        'email': 'user@example.com',
        'access_token': 'access-token',
        'refresh_token': 'refresh-token',
        'expires_in': 86400,
      });

      expect(response.userId, '1');
      expect(response.email, 'user@example.com');
      expect(response.accessToken, 'access-token');
      expect(response.refreshToken, 'refresh-token');
      expect(response.expiresIn, 86400);
    });
  });

  group('RefreshTokenResponse', () {
    test('parses snake_case refresh response', () {
      final response = RefreshTokenResponse.fromJson({
        'access_token': 'access-token',
        'refresh_token': 'refresh-token',
        'expires_in': 86400,
      });

      expect(response.accessToken, 'access-token');
      expect(response.refreshToken, 'refresh-token');
      expect(response.expiresIn, 86400);
    });
  });
}
