import 'package:notio_app/features/auth/data/models/oauth_exchange_request.dart';
import 'package:notio_app/features/auth/data/models/oauth_start_request.dart';
import 'package:notio_app/features/auth/domain/entities/auth_platform.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_session_notifier.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'social_login_action_provider.g.dart';

/// Social login action state
class SocialLoginActionState {
  final bool isLoading;
  final String? error;
  final bool isSuccess;
  final String? authorizationUrl;
  final AuthProvider? activeProvider;

  const SocialLoginActionState({
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
    this.authorizationUrl,
    this.activeProvider,
  });

  SocialLoginActionState copyWith({
    bool? isLoading,
    String? error,
    bool? isSuccess,
    String? authorizationUrl,
    AuthProvider? activeProvider,
  }) {
    return SocialLoginActionState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
      authorizationUrl: authorizationUrl,
      activeProvider: activeProvider ?? this.activeProvider,
    );
  }
}

/// Social login action notifier
@riverpod
class SocialLoginActionNotifier extends _$SocialLoginActionNotifier {
  late final AuthRepository _repository;

  @override
  SocialLoginActionState build() {
    _repository = ref.watch(authRepositoryProvider);
    return const SocialLoginActionState();
  }

  /// Start social login
  Future<void> startSocialLogin(
    AuthProvider provider,
    AuthPlatform platform,
    String redirectUri,
  ) async {
    if (state.isLoading) {
      return;
    }

    state = state.copyWith(
      isLoading: true,
      error: null,
      isSuccess: false,
      authorizationUrl: null,
      activeProvider: provider,
    );

    try {
      final request = OAuthStartRequest(
        provider: provider,
        platform: platform,
        redirectUri: redirectUri,
      );
      final response = await _repository.startSocialLogin(request);

      state = state.copyWith(
        isLoading: false,
        isSuccess: true,
        authorizationUrl: response.authorizationUrl,
        activeProvider: provider,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
        isSuccess: false,
        activeProvider: provider,
      );
    }
  }

  /// Exchange authorization code for tokens
  Future<void> exchangeSocialLogin(
    AuthProvider provider,
    String code,
    String state,
  ) async {
    if (this.state.isLoading) {
      return;
    }

    this.state = this.state.copyWith(
          isLoading: true,
          error: null,
          isSuccess: false,
          activeProvider: provider,
        );

    try {
      final request = OAuthExchangeRequest(
        provider: provider,
        code: code,
        state: state,
      );
      await _repository.exchangeSocialLogin(request);

      // Get user email from stored tokens
      final email = await _repository.getUserEmail();
      if (email != null) {
        // Update session state after successful OAuth login
        ref.read(authSessionNotifierProvider.notifier).setAuthenticated(email);
      }

      this.state = this.state.copyWith(
            isLoading: false,
            isSuccess: true,
            activeProvider: provider,
          );
    } catch (e) {
      this.state = this.state.copyWith(
            isLoading: false,
            error: e.toString(),
            isSuccess: false,
            activeProvider: provider,
          );
    }
  }

  /// Reset state
  void reset() {
    state = const SocialLoginActionState();
  }
}
