import 'package:notio_app/features/auth/data/models/password_reset_request_request.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'password_reset_request_provider.g.dart';

/// Password reset request action state
class PasswordResetRequestState {
  final bool isLoading;
  final String? error;
  final bool isSuccess;
  final String? message;

  const PasswordResetRequestState({
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
    this.message,
  });

  PasswordResetRequestState copyWith({
    bool? isLoading,
    String? error,
    bool? isSuccess,
    String? message,
  }) {
    return PasswordResetRequestState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
      message: message,
    );
  }
}

/// Password reset request action notifier
@riverpod
class PasswordResetRequestNotifier extends _$PasswordResetRequestNotifier {
  late final AuthRepository _repository;

  @override
  PasswordResetRequestState build() {
    _repository = ref.watch(authRepositoryProvider);
    return const PasswordResetRequestState();
  }

  /// Request password reset
  Future<void> requestPasswordReset(String email) async {
    state = state.copyWith(isLoading: true, error: null, isSuccess: false, message: null);

    try {
      final request = PasswordResetRequestRequest(email: email);
      final response = await _repository.requestPasswordReset(request);

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
    state = const PasswordResetRequestState();
  }
}
