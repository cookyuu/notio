import 'package:notio_app/features/auth/data/models/signup_request.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'signup_action_provider.g.dart';

/// Signup action state
class SignupActionState {
  final bool isLoading;
  final String? error;
  final bool isSuccess;

  const SignupActionState({
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
  });

  SignupActionState copyWith({
    bool? isLoading,
    String? error,
    bool? isSuccess,
  }) {
    return SignupActionState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
    );
  }
}

/// Signup action notifier
@riverpod
class SignupActionNotifier extends _$SignupActionNotifier {
  late final AuthRepository _repository;

  @override
  SignupActionState build() {
    _repository = ref.watch(authRepositoryProvider);
    return const SignupActionState();
  }

  /// Signup with email, password, and display name
  Future<void> signup(String email, String password, String displayName) async {
    if (state.isLoading) {
      return;
    }

    state = state.copyWith(isLoading: true, error: null, isSuccess: false);

    try {
      final request = SignupRequest(
        email: email,
        password: password,
        displayName: displayName,
      );
      await _repository.signup(request);

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
    state = const SignupActionState();
  }
}
