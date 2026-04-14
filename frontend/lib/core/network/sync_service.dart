import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/notification/domain/repository/notification_repository.dart';
import 'package:notio_app/features/notification/presentation/providers/notification_providers.dart';
import 'network_status_provider.dart';

/// Sync status
enum SyncStatus {
  idle,
  syncing,
  success,
  error,
}

/// Sync state
class SyncState {
  final SyncStatus status;
  final DateTime? lastSyncTime;
  final String? errorMessage;

  const SyncState({
    required this.status,
    this.lastSyncTime,
    this.errorMessage,
  });

  SyncState copyWith({
    SyncStatus? status,
    DateTime? lastSyncTime,
    String? errorMessage,
  }) {
    return SyncState(
      status: status ?? this.status,
      lastSyncTime: lastSyncTime ?? this.lastSyncTime,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

/// Service for automatic data synchronization
class SyncService extends StateNotifier<SyncState> {
  final NotificationRepository _notificationRepository;
  final Ref _ref;

  SyncService(this._notificationRepository, this._ref)
      : super(const SyncState(status: SyncStatus.idle)) {
    // Listen to network status changes
    _ref.listen<NetworkStatus>(networkStatusProvider, (previous, next) {
      _onNetworkStatusChanged(previous, next);
    });
  }

  /// Handle network status changes
  void _onNetworkStatusChanged(NetworkStatus? previous, NetworkStatus next) {
    // If transitioning from offline to online, trigger sync
    if (previous == NetworkStatus.offline && next == NetworkStatus.online) {
      syncData();
    }
  }

  /// Manually trigger data synchronization
  Future<void> syncData() async {
    if (state.status == SyncStatus.syncing) {
      return; // Already syncing
    }

    state = state.copyWith(status: SyncStatus.syncing, errorMessage: null);

    try {
      // Sync notifications
      await _notificationRepository.fetchNotifications(
        source: null,
        page: 0,
      );

      state = state.copyWith(
        status: SyncStatus.success,
        lastSyncTime: DateTime.now(),
      );
    } catch (e) {
      state = state.copyWith(
        status: SyncStatus.error,
        errorMessage: e.toString(),
      );
    }
  }
}

/// Provider for SyncService
final syncServiceProvider =
    StateNotifierProvider<SyncService, SyncState>((ref) {
  final notificationRepository = ref.watch(notificationRepositoryProvider);
  return SyncService(notificationRepository, ref);
});

/// Provider for checking if currently syncing
final isSyncingProvider = Provider<bool>((ref) {
  final syncState = ref.watch(syncServiceProvider);
  return syncState.status == SyncStatus.syncing;
});
