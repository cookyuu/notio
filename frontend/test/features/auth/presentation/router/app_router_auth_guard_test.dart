import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/router/app_router.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:notio_app/features/auth/presentation/screens/auth_oauth_callback_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/find_id_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_confirm_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_request_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/signup_screen.dart';

import '../auth_test_helpers.dart';

void main() {
  group('App router auth guard', () {
    testWidgets('allows unauthenticated access to new public auth routes',
        (tester) async {
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => FakeAuthRepository()),
        ],
      );
      final router = container.read(goRouterProvider);

      await pumpRouterApp(tester, router: router, container: container);
      await tester.pumpAndSettle();

      final publicRoutes = <String, Finder>{
        Routes.signup: find.byType(SignupScreen),
        Routes.findId: find.byType(FindIdScreen),
        Routes.resetPasswordRequest: find.byType(PasswordResetRequestScreen),
        '${Routes.resetPasswordConfirm}/route-token':
            find.byType(PasswordResetConfirmScreen),
        '${Routes.authOAuthCallback}?error=access_denied':
            find.byType(AuthOAuthCallbackScreen),
      };

      for (final entry in publicRoutes.entries) {
        router.go(entry.key);
        await tester.pumpAndSettle();

        expect(entry.value, findsOneWidget);
      }
    });
  });
}
