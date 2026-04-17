import 'package:notio_app/features/auth/data/models/password_reset_confirm_request.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'password_reset_confirm_provider.g.dart';

/// Password reset confirm action state
class PasswordResetConfirmState {
  final bool isLoading;
  final String? error;
  final bool isSuccess;
  final String? message;

  const PasswordResetConfirmState({
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
    this.message,
  });

  PasswordResetConfirmState copyWith({
    bool? isLoading,
    String? error,
    bool? isSuccess,
    String? message,
  }) {
    return PasswordResetConfirmState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
      message: message,
    );
  }
}

/// Password reset confirm action notifier
@riverpod
class PasswordResetConfirmNotifier extends _$PasswordResetConfirmNotifier {
  late final AuthRepository _repository;

  @override
  PasswordResetConfirmState build() {
    _repository = ref.watch(authRepositoryProvider);
    return const PasswordResetConfirmState();
  }

  /// Confirm password reset
  Future<void> confirmPasswordReset(String token, String newPassword) async {
    state = state.copyWith(isLoading: true, error: null, isSuccess: false, message: null);

    try {
      final request = PasswordResetConfirmRequest(
        token: token,
        newPassword: newPassword,
      );
      final response = await _repository.confirmPasswordReset(request);

      state = state.copyWith(
        isLoading: false,
        isSuccess: true,
        message: response.message,
      );
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
    state = const PasswordResetConfirmState();
  }
}
