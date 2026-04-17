import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_session_notifier.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'login_action_provider.g.dart';

/// Login action state
class LoginActionState {
  final bool isLoading;
  final String? error;
  final bool isSuccess;

  const LoginActionState({
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
  });

  LoginActionState copyWith({
    bool? isLoading,
    String? error,
    bool? isSuccess,
  }) {
    return LoginActionState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
    );
  }
}

/// Login action notifier
@riverpod
class LoginActionNotifier extends _$LoginActionNotifier {
  late final AuthRepository _repository;

  @override
  LoginActionState build() {
    _repository = ref.watch(authRepositoryProvider);
    return const LoginActionState();
  }

  /// Login with email and password
  Future<void> login(String email, String password) async {
    if (state.isLoading) {
      return;
    }

    state = state.copyWith(isLoading: true, error: null, isSuccess: false);

    try {
      final request = LoginRequest(email: email, password: password);
      final response = await _repository.login(request);

      // Update session state
      ref
          .read(authSessionNotifierProvider.notifier)
          .setAuthenticated(response.email);

      state = state.copyWith(isLoading: false, isSuccess: true);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
        isSuccess: false,
      );
    }
  }

  /// Reset state
  void reset() {
    state = const LoginActionState();
  }
}
