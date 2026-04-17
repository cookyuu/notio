import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';
import 'package:notio_app/features/auth/domain/entities/social_login_launch_mode.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_config_provider.dart';
import 'package:notio_app/features/auth/presentation/screens/auth_oauth_callback_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/find_id_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/login_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_confirm_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_request_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/signup_screen.dart';

import '../auth_test_helpers.dart';

void main() {
  group('Auth screens', () {
    testWidgets('login screen shows navigation actions and routes correctly',
        (tester) async {
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => FakeAuthRepository()),
          socialLoginProviderConfigsProvider.overrideWith((ref) => const []),
        ],
      );
      final router = GoRouter(
        initialLocation: Routes.login,
        routes: [
          GoRoute(
            path: Routes.login,
            builder: (context, state) => const LoginScreen(),
          ),
          GoRoute(
            path: Routes.signup,
            builder: (context, state) => const SignupScreen(),
          ),
          GoRoute(
            path: Routes.findId,
            builder: (context, state) => const FindIdScreen(),
          ),
          GoRoute(
            path: Routes.resetPasswordRequest,
            builder: (context, state) => const PasswordResetRequestScreen(),
          ),
        ],
      );
      addTearDown(router.dispose);

      await pumpRouterApp(tester, router: router, container: container);
      await tester.pumpAndSettle();

      expect(find.text('회원가입'), findsWidgets);
      expect(find.text('아이디 찾기'), findsOneWidget);
      expect(find.text('비밀번호 찾기'), findsOneWidget);

      await tester.tap(find.widgetWithText(TextButton, '회원가입'));
      await tester.pumpAndSettle();
      expect(find.byType(SignupScreen), findsOneWidget);

      router.go(Routes.login);
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(TextButton, '아이디 찾기'));
      await tester.pumpAndSettle();
      expect(find.byType(FindIdScreen), findsOneWidget);

      router.go(Routes.login);
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(TextButton, '비밀번호 찾기'));
      await tester.pumpAndSettle();
      expect(find.byType(PasswordResetRequestScreen), findsOneWidget);
    });

    testWidgets('auth screens navigate back to login', (tester) async {
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => FakeAuthRepository()),
        ],
      );
      final router = GoRouter(
        initialLocation: Routes.signup,
        routes: [
          GoRoute(
            path: Routes.login,
            builder: (context, state) => const LoginScreen(),
          ),
          GoRoute(
            path: Routes.signup,
            builder: (context, state) => const SignupScreen(),
          ),
          GoRoute(
            path: Routes.findId,
            builder: (context, state) => const FindIdScreen(),
          ),
          GoRoute(
            path: Routes.resetPasswordRequest,
            builder: (context, state) => const PasswordResetRequestScreen(),
          ),
          GoRoute(
            path: Routes.resetPasswordConfirm,
            builder: (context, state) => const PasswordResetConfirmScreen(),
          ),
          GoRoute(
            path: Routes.authOAuthCallback,
            builder: (context, state) => const AuthOAuthCallbackScreen(),
          ),
        ],
      );
      addTearDown(router.dispose);

      await pumpRouterApp(tester, router: router, container: container);
      await tester.pumpAndSettle();

      for (final route in [
        Routes.signup,
        Routes.findId,
        Routes.resetPasswordRequest,
        Routes.resetPasswordConfirm,
        '${Routes.authOAuthCallback}?error=denied',
      ]) {
        router.go(route);
        await tester.pumpAndSettle();

        final backButton = find.widgetWithText(TextButton, '로그인으로 돌아가기');
        await tester.ensureVisible(backButton);
        await tester.tap(backButton);
        await tester.pumpAndSettle();

        expect(find.byType(LoginScreen), findsOneWidget);
      }
    });

    testWidgets('signup validates email and password confirmation',
        (tester) async {
      final repository = FakeAuthRepository();
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => repository),
        ],
      );

      await pumpTestApp(
        tester,
        container: container,
        child: const SignupScreen(),
      );
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField).at(0), 'Tester');
      await tester.enterText(find.byType(TextField).at(1), 'invalid-email');
      await tester.enterText(find.byType(TextField).at(2), 'Abcd1234!');
      await tester.enterText(find.byType(TextField).at(3), 'Abcd1234!');
      final signupButton = find.widgetWithText(FilledButton, '회원가입');
      await tester.ensureVisible(signupButton);
      await tester.tap(signupButton);
      await tester.pump();

      expect(find.text('올바른 이메일 형식을 입력해주세요.'), findsOneWidget);
      expect(repository.signupCalls, 0);

      await tester.enterText(find.byType(TextField).at(1), 'tester@example.com');
      await tester.enterText(find.byType(TextField).at(3), 'Mismatch123!');
      await tester.ensureVisible(signupButton);
      await tester.tap(signupButton);
      await tester.pump();

      expect(find.text('비밀번호가 일치하지 않습니다.'), findsWidgets);
      expect(repository.signupCalls, 0);
    });

    testWidgets('find id screen shows success state after request',
        (tester) async {
      final repository = FakeAuthRepository();
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => repository),
        ],
      );

      await pumpTestApp(
        tester,
        container: container,
        child: const FindIdScreen(),
      );
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField), 'tester@example.com');
      await tester.tap(find.widgetWithText(FilledButton, '아이디 찾기'));
      await tester.pumpAndSettle();

      expect(repository.findIdCalls, 1);
      expect(repository.lastFindIdRequest?.email, 'tester@example.com');
      expect(find.text('입력한 이메일로 가입 정보 안내를 전송했습니다.'), findsOneWidget);
    });

    testWidgets('password reset request screen submits and shows success',
        (tester) async {
      final repository = FakeAuthRepository();
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => repository),
        ],
      );

      await pumpTestApp(
        tester,
        container: container,
        child: const PasswordResetRequestScreen(),
      );
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField), 'tester@example.com');
      await tester.tap(find.widgetWithText(FilledButton, '재설정 링크 보내기'));
      await tester.pumpAndSettle();

      expect(repository.passwordResetRequestCalls, 1);
      expect(
        repository.lastPasswordResetRequest?.email,
        'tester@example.com',
      );
      expect(find.text('비밀번호 재설정 안내를 전송했습니다.'), findsOneWidget);
    });

    testWidgets('password reset confirm screen reads token and submits',
        (tester) async {
      final repository = FakeAuthRepository();
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => repository),
        ],
      );
      final router = GoRouter(
        initialLocation: '${Routes.resetPasswordConfirm}?token=query-token',
        routes: [
          GoRoute(
            path: Routes.login,
            builder: (context, state) => const LoginScreen(),
          ),
          GoRoute(
            path: Routes.resetPasswordConfirm,
            builder: (context, state) => const PasswordResetConfirmScreen(),
          ),
        ],
      );
      addTearDown(router.dispose);

      await pumpRouterApp(tester, router: router, container: container);
      await tester.pumpAndSettle();

      expect(find.text('query-token'), findsOneWidget);

      await tester.enterText(find.byType(TextField).at(1), 'Abcd1234!');
      await tester.enterText(find.byType(TextField).at(2), 'Abcd1234!');
      final submitButton = find.widgetWithText(FilledButton, '비밀번호 변경');
      await tester.ensureVisible(submitButton);
      await tester.tap(submitButton);
      await tester.pumpAndSettle();

      expect(repository.passwordResetConfirmCalls, 1);
      expect(
        repository.lastPasswordResetConfirmRequest?.token,
        'query-token',
      );
      expect(find.byType(LoginScreen), findsOneWidget);
    });

    testWidgets('social login buttons follow visibility config', (tester) async {
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => FakeAuthRepository()),
          socialLoginProviderConfigsProvider.overrideWith(
            (ref) => [
              const SocialLoginProviderConfig(
                provider: AuthProvider.google,
                label: 'Google',
                icon: Icons.public,
                backgroundColor: Colors.white,
                foregroundColor: Colors.black,
                clientConfig: SocialLoginClientConfig(
                  webClientId: 'google-web-client',
                ),
                launchMode: SocialLoginLaunchMode.redirect,
              ),
            ],
          ),
        ],
      );

      await pumpTestApp(
        tester,
        container: container,
        child: const LoginScreen(),
      );
      await tester.pumpAndSettle();

      expect(find.text('소셜 로그인'), findsOneWidget);
      expect(find.text('Google로 계속하기'), findsOneWidget);
      expect(find.text('Apple로 계속하기'), findsNothing);
    });

    testWidgets('oauth callback parses provider code and state', (tester) async {
      final repository = FakeAuthRepository();
      final container = createContainer(
        overrides: [
          authRepositoryProvider.overrideWith((ref) => repository),
        ],
      );
      final router = GoRouter(
        initialLocation:
            '${Routes.authOAuthCallback}?provider=google&code=auth-code&state=csrf-state',
        routes: [
          GoRoute(
            path: Routes.authOAuthCallback,
            builder: (context, state) => const AuthOAuthCallbackScreen(),
          ),
          GoRoute(
            path: Routes.notifications,
            builder: (context, state) => const Scaffold(
              body: Text('notifications'),
            ),
          ),
          GoRoute(
            path: Routes.login,
            builder: (context, state) => const LoginScreen(),
          ),
        ],
      );
      addTearDown(router.dispose);

      await pumpRouterApp(tester, router: router, container: container);
      await tester.pump();
      await tester.pumpAndSettle();

      expect(repository.oauthExchangeCalls, 1);
      expect(
        repository.lastOAuthExchangeRequest?.provider,
        AuthProvider.google,
      );
      expect(repository.lastOAuthExchangeRequest?.code, 'auth-code');
      expect(repository.lastOAuthExchangeRequest?.state, 'csrf-state');
      expect(find.text('notifications'), findsOneWidget);
    });
  });
}
