import 'package:notio_app/features/auth/data/models/find_id_request.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'find_id_action_provider.g.dart';

/// Find ID action state
class FindIdActionState {
  final bool isLoading;
  final String? error;
  final bool isSuccess;
  final String? message;

  const FindIdActionState({
    this.isLoading = false,
    this.error,
    this.isSuccess = false,
    this.message,
  });

  FindIdActionState copyWith({
    bool? isLoading,
    String? error,
    bool? isSuccess,
    String? message,
  }) {
    return FindIdActionState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      isSuccess: isSuccess ?? this.isSuccess,
      message: message,
    );
  }
}

/// Find ID action notifier
@riverpod
class FindIdActionNotifier extends _$FindIdActionNotifier {
  late final AuthRepository _repository;

  @override
  FindIdActionState build() {
    _repository = ref.watch(authRepositoryProvider);
    return const FindIdActionState();
  }

  /// Find ID by email
  Future<void> findId(String email) async {
    state = state.copyWith(isLoading: true, error: null, isSuccess: false, message: null);

    try {
      final request = FindIdRequest(email: email);
      final response = await _repository.findId(request);

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
    state = const FindIdActionState();
  }
}
