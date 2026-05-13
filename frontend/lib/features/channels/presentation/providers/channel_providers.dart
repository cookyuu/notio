import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:notio_app/features/channels/data/datasource/channel_remote_datasource.dart';
import 'package:notio_app/features/channels/data/repository/channel_repository_impl.dart';
import 'package:notio_app/features/channels/domain/entity/notification_channel_entity.dart';
import 'package:notio_app/features/channels/domain/repository/channel_repository.dart';
import 'package:notio_app/features/notification/presentation/providers/notification_providers.dart';

part 'channel_providers.g.dart';

final channelRemoteDataSourceProvider =
    Provider<ChannelRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return ChannelRemoteDataSource(dio);
});

final channelRepositoryProvider = Provider<ChannelRepository>((ref) {
  return ChannelRepositoryImpl(
    remoteDataSource: ref.watch(channelRemoteDataSourceProvider),
  );
});

class ChannelState {
  final List<NotificationChannelEntity> channels;
  final bool isLoading;
  final bool isActing;
  final String? error;
  final String? successMessage;

  const ChannelState({
    this.channels = const [],
    this.isLoading = false,
    this.isActing = false,
    this.error,
    this.successMessage,
  });

  ChannelState copyWith({
    List<NotificationChannelEntity>? channels,
    bool? isLoading,
    bool? isActing,
    String? error,
    String? successMessage,
    bool clearError = false,
    bool clearSuccess = false,
  }) {
    return ChannelState(
      channels: channels ?? this.channels,
      isLoading: isLoading ?? this.isLoading,
      isActing: isActing ?? this.isActing,
      error: clearError ? null : (error ?? this.error),
      successMessage:
          clearSuccess ? null : (successMessage ?? this.successMessage),
    );
  }
}

@riverpod
class ChannelNotifier extends _$ChannelNotifier {
  @override
  ChannelState build() => const ChannelState();

  Future<void> load() async {
    if (state.isLoading) return;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final channels =
          await ref.read(channelRepositoryProvider).fetchChannels();
      state = state.copyWith(channels: channels, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  Future<void> toggleStatus(int id, bool active) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      final repo = ref.read(channelRepositoryProvider);
      if (active) {
        await repo.resumeChannel(id);
      } else {
        await repo.pauseChannel(id);
      }
      await load();
      state = state.copyWith(isActing: false);
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
    }
  }

  Future<bool> sendTest(int id) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      await ref.read(channelRepositoryProvider).testChannel(id);
      state = state.copyWith(
        isActing: false,
        successMessage: '테스트 메시지를 전송했습니다.',
      );
      return true;
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
      return false;
    }
  }

  Future<bool> createChannel({
    required String displayName,
    required String channelType,
    required String credentialPlaintext,
    String? targetIdentifier,
  }) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      await ref.read(channelRepositoryProvider).createChannel(
            displayName: displayName,
            channelType: channelType,
            credentialPlaintext: credentialPlaintext,
            targetIdentifier: targetIdentifier,
          );
      await load();
      return true;
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
      return false;
    }
  }

  Future<void> deleteChannel(int id) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      await ref.read(channelRepositoryProvider).deleteChannel(id);
      await load();
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
    }
  }

  void clearMessages() {
    state = state.copyWith(clearError: true, clearSuccess: true);
  }
}
