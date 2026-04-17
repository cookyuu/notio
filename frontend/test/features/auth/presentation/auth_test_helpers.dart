import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/theme/app_theme.dart';
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
import 'package:notio_app/features/auth/domain/entities/auth_platform.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';
import 'package:notio_app/features/auth/domain/entities/social_login_launch_mode.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_entry_strategy_provider.dart';

class FakeAuthRepository implements AuthRepository {
  FakeAuthRepository({
    this.isLoggedInResult = false,
    this.userEmail = 'tester@example.com',
    this.signupResponse = const SignupResponse(message: '회원가입 완료'),
    this.findIdResponse = const FindIdResponse(message: '아이디 안내 전송'),
    this.passwordResetRequestResponse = const PasswordResetRequestResponse(
      message: '재설정 링크 전송',
    ),
    this.passwordResetConfirmResponse = const PasswordResetConfirmResponse(
      message: '비밀번호가 변경되었습니다.',
    ),
    OAuthStartResponse? oauthStartResponse,
    OAuthExchangeResponse? oauthExchangeResponse,
    this.loginError,
    this.signupError,
    this.findIdError,
    this.passwordResetRequestError,
    this.passwordResetConfirmError,
    this.oauthStartError,
    this.oauthExchangeError,
  })  : oauthStartResponse = oauthStartResponse ??
            OAuthStartResponse(
              provider: AuthProvider.google,
              platform: AuthPlatform.web,
              state: 'state-value',
              authorizationUrl: 'https://example.com/oauth',
              expiresAt: DateTime(2030),
            ),
        oauthExchangeResponse = oauthExchangeResponse ??
            const OAuthExchangeResponse(
              provider: AuthProvider.google,
              state: 'state-value',
              message: '로그인 완료',
            );

  final bool isLoggedInResult;
  final String? userEmail;
  final SignupResponse signupResponse;
  final FindIdResponse findIdResponse;
  final PasswordResetRequestResponse passwordResetRequestResponse;
  final PasswordResetConfirmResponse passwordResetConfirmResponse;
  final OAuthStartResponse oauthStartResponse;
  final OAuthExchangeResponse oauthExchangeResponse;
  final Object? loginError;
  final Object? signupError;
  final Object? findIdError;
  final Object? passwordResetRequestError;
  final Object? passwordResetConfirmError;
  final Object? oauthStartError;
  final Object? oauthExchangeError;

  LoginRequest? lastLoginRequest;
  SignupRequest? lastSignupRequest;
  FindIdRequest? lastFindIdRequest;
  PasswordResetRequestRequest? lastPasswordResetRequest;
  PasswordResetConfirmRequest? lastPasswordResetConfirmRequest;
  OAuthStartRequest? lastOAuthStartRequest;
  OAuthExchangeRequest? lastOAuthExchangeRequest;

  int loginCalls = 0;
  int signupCalls = 0;
  int findIdCalls = 0;
  int passwordResetRequestCalls = 0;
  int passwordResetConfirmCalls = 0;
  int oauthStartCalls = 0;
  int oauthExchangeCalls = 0;

  @override
  Future<LoginResponse> login(LoginRequest request) async {
    loginCalls += 1;
    lastLoginRequest = request;
    if (loginError != null) {
      throw loginError!;
    }

    return const LoginResponse(
      userId: 'user-1',
      email: 'tester@example.com',
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    );
  }

  @override
  Future<void> refreshToken() async {}

  @override
  Future<void> logout() async {}

  @override
  Future<bool> isLoggedIn() async => isLoggedInResult;

  @override
  Future<String?> getAccessToken() async => 'access-token';

  @override
  Future<String?> getRefreshToken() async => 'refresh-token';

  @override
  Future<String?> getUserEmail() async => userEmail;

  @override
  Future<SignupResponse> signup(SignupRequest request) async {
    signupCalls += 1;
    lastSignupRequest = request;
    if (signupError != null) {
      throw signupError!;
    }
    return signupResponse;
  }

  @override
  Future<FindIdResponse> findId(FindIdRequest request) async {
    findIdCalls += 1;
    lastFindIdRequest = request;
    if (findIdError != null) {
      throw findIdError!;
    }
    return findIdResponse;
  }

  @override
  Future<PasswordResetRequestResponse> requestPasswordReset(
    PasswordResetRequestRequest request,
  ) async {
    passwordResetRequestCalls += 1;
    lastPasswordResetRequest = request;
    if (passwordResetRequestError != null) {
      throw passwordResetRequestError!;
    }
    return passwordResetRequestResponse;
  }

  @override
  Future<PasswordResetConfirmResponse> confirmPasswordReset(
    PasswordResetConfirmRequest request,
  ) async {
    passwordResetConfirmCalls += 1;
    lastPasswordResetConfirmRequest = request;
    if (passwordResetConfirmError != null) {
      throw passwordResetConfirmError!;
    }
    return passwordResetConfirmResponse;
  }

  @override
  Future<OAuthStartResponse> startSocialLogin(OAuthStartRequest request) async {
    oauthStartCalls += 1;
    lastOAuthStartRequest = request;
    if (oauthStartError != null) {
      throw oauthStartError!;
    }
    return oauthStartResponse;
  }

  @override
  Future<OAuthExchangeResponse> exchangeSocialLogin(
    OAuthExchangeRequest request,
  ) async {
    oauthExchangeCalls += 1;
    lastOAuthExchangeRequest = request;
    if (oauthExchangeError != null) {
      throw oauthExchangeError!;
    }
    return oauthExchangeResponse;
  }
}

class FakeSocialLoginEntryStrategy implements SocialLoginEntryStrategy {
  FakeSocialLoginEntryStrategy({
    this.launchResult = true,
  });

  final bool launchResult;
  String? lastAuthorizationUrl;

  @override
  SocialLoginLaunchMode get launchMode => SocialLoginLaunchMode.redirect;

  @override
  Future<bool> launchAuthorizationUrl(String authorizationUrl) async {
    lastAuthorizationUrl = authorizationUrl;
    return launchResult;
  }
}

ProviderContainer createContainer({
  List<Override> overrides = const [],
}) {
  final container = ProviderContainer(overrides: overrides);
  addTearDown(container.dispose);
  return container;
}

Future<void> pumpTestApp(
  WidgetTester tester, {
  required Widget child,
  ProviderContainer? container,
}) async {
  final scope = container == null
      ? ProviderScope(
          child: MaterialApp(
            theme: AppTheme.darkTheme,
            home: child,
          ),
        )
      : UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            theme: AppTheme.darkTheme,
            home: child,
          ),
        );

  await tester.pumpWidget(scope);
}

Future<void> pumpRouterApp(
  WidgetTester tester, {
  required GoRouter router,
  ProviderContainer? container,
}) async {
  final child = MaterialApp.router(
    theme: AppTheme.darkTheme,
    routerConfig: router,
  );

  await tester.pumpWidget(
    container == null
        ? ProviderScope(child: child)
        : UncontrolledProviderScope(container: container, child: child),
  );
}
