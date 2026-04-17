import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/domain/auth_route_policy.dart';

void main() {
  group('AuthRoutePolicy', () {
    test('treats reset-password confirm token path as public route', () {
      expect(
        AuthRoutePolicy.isPublicRoute('/reset-password/confirm/sample-token'),
        isTrue,
      );
    });

    test('extracts reset token from query parameter', () {
      final token = AuthRoutePolicy.extractResetToken(
        uri: Uri.parse('/reset-password/confirm?token=query-token'),
      );

      expect(token, 'query-token');
    });

    test('extracts reset token from path segment', () {
      final token = AuthRoutePolicy.extractResetToken(
        uri: Uri.parse('/reset-password/confirm/path-token'),
      );

      expect(token, 'path-token');
    });

    test('extracts reset token from deep link fragment', () {
      final token = AuthRoutePolicy.extractResetToken(
        uri: Uri.parse(
            'https://app.notio.dev/#/reset-password/confirm?token=fragment-token'),
      );

      expect(token, 'fragment-token');
    });

    test('allows authenticated users to access reset confirm with token', () {
      final canAccess = AuthRoutePolicy.canAuthenticatedUserAccessAuthRoute(
        path: Routes.resetPasswordConfirm,
        uri: Uri.parse('/reset-password/confirm?token=query-token'),
      );

      expect(canAccess, isTrue);
    });

    test('redirects authenticated users away from login', () {
      final canAccess = AuthRoutePolicy.canAuthenticatedUserAccessAuthRoute(
        path: Routes.login,
        uri: Uri.parse(Routes.login),
      );

      expect(canAccess, isFalse);
    });
  });
}
