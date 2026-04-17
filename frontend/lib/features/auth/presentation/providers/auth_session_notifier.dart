import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'auth_session_notifier.g.dart';

/// Auth session state - 세션 상태만 관리 (액션은 별도 provider로 분리)
class AuthSessionState {
  final bool isAuthenticated;
  final String? userEmail;

  const AuthSessionState({
    this.isAuthenticated = false,
    this.userEmail,
  });

  AuthSessionState copyWith({
    bool? isAuthenticated,
    String? userEmail,
  }) {
    return AuthSessionState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      userEmail: userEmail ?? this.userEmail,
    );
  }
}

/// Auth session notifier - 세션 상태 전용
@Riverpod(keepAlive: true)
class AuthSessionNotifier extends _$AuthSessionNotifier {
  late final AuthRepository _repository;

  @override
  Future<AuthSessionState> build() async {
    _repository = ref.watch(authRepositoryProvider);

    // Check if user is already logged in
    final isLoggedIn = await _repository.isLoggedIn();
    if (isLoggedIn) {
      final email = await _repository.getUserEmail();
      return AuthSessionState(
        isAuthenticated: true,
        userEmail: email,
      );
    }

    return const AuthSessionState();
  }

  /// Update session after successful login
  void setAuthenticated(String email) {
    state = AsyncValue.data(
      AuthSessionState(
        isAuthenticated: true,
        userEmail: email,
      ),
    );
  }

  /// Clear session
  Future<void> clearSession() async {
    try {
      await _repository.logout();
      state = const AsyncValue.data(AuthSessionState());
    } catch (e) {
      // Even if logout API fails, clear local session
      state = const AsyncValue.data(AuthSessionState());
    }
  }

  /// Refresh token
  Future<void> refreshToken() async {
    try {
      await _repository.refreshToken();
    } catch (e) {
      // If refresh fails, clear session
      await clearSession();
    }
  }
}
