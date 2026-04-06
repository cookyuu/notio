import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'auth_notifier.g.dart';

/// Auth state
class AuthState {
  final bool isAuthenticated;
  final String? userEmail;
  final bool isLoading;
  final String? error;

  const AuthState({
    this.isAuthenticated = false,
    this.userEmail,
    this.isLoading = false,
    this.error,
  });

  AuthState copyWith({
    bool? isAuthenticated,
    String? userEmail,
    bool? isLoading,
    String? error,
  }) {
    return AuthState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      userEmail: userEmail ?? this.userEmail,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

@riverpod
class AuthNotifier extends _$AuthNotifier {
  late final AuthRepository _repository;

  @override
  Future<AuthState> build() async {
    _repository = ref.watch(authRepositoryProvider);

    // Check if user is already logged in
    final isLoggedIn = await _repository.isLoggedIn();
    if (isLoggedIn) {
      final email = await _repository.getUserEmail();
      return AuthState(
        isAuthenticated: true,
        userEmail: email,
      );
    }

    return const AuthState();
  }

  /// Login with email and password
  Future<void> login(String email, String password) async {
    state = const AsyncValue.loading();

    state = await AsyncValue.guard(() async {
      final request = LoginRequest(email: email, password: password);
      final response = await _repository.login(request);

      return AuthState(
        isAuthenticated: true,
        userEmail: response.email,
      );
    });
  }

  /// Logout
  Future<void> logout() async {
    state = AsyncValue.data(state.value!.copyWith(isLoading: true));

    try {
      await _repository.logout();
      state = const AsyncValue.data(AuthState());
    } catch (e) {
      state = AsyncValue.data(
        state.value!.copyWith(
          isLoading: false,
          error: e.toString(),
        ),
      );
    }
  }

  /// Refresh token
  Future<void> refreshToken() async {
    try {
      await _repository.refreshToken();
    } catch (e) {
      // If refresh fails, logout user
      await logout();
    }
  }
}
