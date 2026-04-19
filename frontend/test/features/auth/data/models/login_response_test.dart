import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';

void main() {
  group('LoginResponse', () {
    test('parses nested login response', () {
      final response = LoginResponse.fromJson({
        'access_token': 'access-token',
        'refresh_token': 'refresh-token',
        'token_type': 'Bearer',
        'expires_in': 86400,
        'user': {
          'id': 1,
          'primary_email': 'user@example.com',
          'display_name': 'Notio User',
          'status': 'ACTIVE',
        },
      });

      expect(response.userId, '1');
      expect(response.email, 'user@example.com');
      expect(response.accessToken, 'access-token');
      expect(response.refreshToken, 'refresh-token');
      expect(response.tokenType, 'Bearer');
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

    test('parses camelCase refresh response', () {
      final response = RefreshTokenResponse.fromJson({
        'accessToken': 'access-token',
        'refreshToken': 'refresh-token',
        'tokenType': 'Bearer',
        'expiresIn': 86400,
      });

      expect(response.accessToken, 'access-token');
      expect(response.refreshToken, 'refresh-token');
      expect(response.tokenType, 'Bearer');
      expect(response.expiresIn, 86400);
    });
  });
}
