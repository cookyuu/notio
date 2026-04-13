import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/domain/auth_token_policy.dart';

void main() {
  group('AuthTokenPolicy', () {
    test('returns true for mock tokens', () {
      expect(
        AuthTokenPolicy.isMockToken('mock-access-token-refreshed-123'),
        isTrue,
      );
    });

    test('returns false for real tokens', () {
      expect(AuthTokenPolicy.isMockToken('eyJhbGciOiJIUzI1NiJ9'), isFalse);
    });

    test('returns false for null', () {
      expect(AuthTokenPolicy.isMockToken(null), isFalse);
    });
  });
}
