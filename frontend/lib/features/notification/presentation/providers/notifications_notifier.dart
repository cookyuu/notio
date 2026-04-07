import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/repository/notification_repository.dart';
import 'package:notio_app/features/notification/presentation/providers/notification_providers.dart';

/// Notifications state
class NotificationsState {
  final List<NotificationEntity> notifications;
  final bool isLoading;
  final String? error;
  final NotificationSource? selectedSource;
  final int page;
  final bool hasMore;

  const NotificationsState({
    this.notifications = const [],
    this.isLoading = false,
    this.error,
    this.selectedSource,
    this.page = 0,
    this.hasMore = true,
  });

  NotificationsState copyWith({
    List<NotificationEntity>? notifications,
    bool? isLoading,
    String? error,
    NotificationSource? selectedSource,
    int? page,
    bool? hasMore,
  }) {
    return NotificationsState(
      notifications: notifications ?? this.notifications,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      selectedSource: selectedSource ?? this.selectedSource,
      page: page ?? this.page,
      hasMore: hasMore ?? this.hasMore,
    );
  }
}

/// Notifications Notifier
class NotificationsNotifier extends StateNotifier<NotificationsState> {
  final NotificationRepository _repository;

  NotificationsNotifier(this._repository) : super(const NotificationsState());

  /// Fetch notifications
  Future<void> fetchNotifications({bool refresh = false}) async {
    if (state.isLoading) return;

    try {
      state = state.copyWith(
        isLoading: true,
        error: null,
      );

      final page = refresh ? 0 : state.page;
      final notifications = await _repository.fetchNotifications(
        source: state.selectedSource,
        page: page,
        size: 20,
      );

      if (refresh) {
        state = state.copyWith(
          notifications: notifications,
          isLoading: false,
          page: 0,
          hasMore: notifications.length >= 20,
        );
      } else {
        state = state.copyWith(
          notifications: [...state.notifications, ...notifications],
          isLoading: false,
          page: page + 1,
          hasMore: notifications.length >= 20,
        );
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  /// Set source filter
  Future<void> setSourceFilter(NotificationSource? source) async {
    state = state.copyWith(
      selectedSource: source,
      notifications: [],
      page: 0,
      hasMore: true,
    );
    await fetchNotifications(refresh: true);
  }

  /// Mark as read
  Future<void> markAsRead(int notificationId) async {
    try {
      // Optimistic update
      final updatedNotifications = state.notifications.map((notification) {
        if (notification.id == notificationId) {
          return notification.copyWith(isRead: true);
        }
        return notification;
      }).toList();

      state = state.copyWith(notifications: updatedNotifications);

      // Update on server
      await _repository.markAsRead(notificationId);
    } catch (e) {
      // Revert on error
      await fetchNotifications(refresh: true);
    }
  }

  /// Mark all as read
  Future<void> markAllAsRead() async {
    try {
      // Optimistic update
      final updatedNotifications = state.notifications.map((notification) {
        return notification.copyWith(isRead: true);
      }).toList();

      state = state.copyWith(notifications: updatedNotifications);

      // Update on server
      await _repository.markAllAsRead();
    } catch (e) {
      // Revert on error
      await fetchNotifications(refresh: true);
    }
  }

  /// Delete notification
  Future<void> deleteNotification(int notificationId) async {
    try {
      // Optimistic update
      final updatedNotifications = state.notifications
          .where((notification) => notification.id != notificationId)
          .toList();

      state = state.copyWith(notifications: updatedNotifications);

      // Delete on server
      await _repository.deleteNotification(notificationId);
    } catch (e) {
      // Revert on error
      await fetchNotifications(refresh: true);
    }
  }

  /// Load more notifications (pagination)
  Future<void> loadMore() async {
    if (!state.hasMore || state.isLoading) return;
    await fetchNotifications();
  }

  /// Add test notification (for developer menu)
  void addTestNotification(NotificationEntity notification) {
    final updatedNotifications = [notification, ...state.notifications];
    state = state.copyWith(notifications: updatedNotifications);
  }

  /// Clear cache (for developer menu)
  Future<void> clearCache() async {
    try {
      await _repository.clearCache();
      state = state.copyWith(
        notifications: [],
        page: 0,
        hasMore: true,
      );
    } catch (e) {
      state = state.copyWith(error: e.toString());
    }
  }
}

/// Notifications Provider
final notificationsProvider =
    StateNotifierProvider<NotificationsNotifier, NotificationsState>((ref) {
  final repository = ref.watch(notificationRepositoryProvider);
  return NotificationsNotifier(repository);
});

/// Unread count provider
final unreadCountProvider = FutureProvider<int>((ref) async {
  final repository = ref.watch(notificationRepositoryProvider);
  return repository.getUnreadCount();
});
