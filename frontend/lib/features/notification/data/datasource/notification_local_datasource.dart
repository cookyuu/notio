import 'package:notio_app/features/notification/data/model/notification_model.dart';

/// Simple in-memory local data source for notifications
/// TODO: Replace with Drift implementation once build_runner compatibility is resolved
class NotificationLocalDataSource {
  final Map<int, NotificationModel> _cache = {};

  /// Get all cached notifications, optionally filtered by source
  Future<List<NotificationModel>> getCachedNotifications({
    String? source,
  }) async {
    var notifications = _cache.values.toList();

    if (source != null) {
      notifications = notifications.where((n) => n.source == source).toList();
    }

    notifications.sort((a, b) {
      return DateTime.parse(b.createdAt).compareTo(DateTime.parse(a.createdAt));
    });

    return notifications;
  }

  /// Save notifications to cache
  Future<void> cacheNotifications(List<NotificationModel> notifications) async {
    for (final notification in notifications) {
      _cache[notification.id] = notification;
    }
  }

  /// Mark a notification as read
  Future<void> markAsRead(int notificationId) async {
    final notification = _cache[notificationId];
    if (notification != null) {
      _cache[notificationId] = NotificationModel(
        id: notification.id,
        source: notification.source,
        title: notification.title,
        body: notification.body,
        priority: notification.priority,
        isRead: true,
        createdAt: notification.createdAt,
        externalId: notification.externalId,
        externalUrl: notification.externalUrl,
        metadata: notification.metadata,
      );
    }
  }

  /// Mark all notifications as read
  Future<void> markAllAsRead() async {
    final updatedCache = <int, NotificationModel>{};

    for (final entry in _cache.entries) {
      updatedCache[entry.key] = NotificationModel(
        id: entry.value.id,
        source: entry.value.source,
        title: entry.value.title,
        body: entry.value.body,
        priority: entry.value.priority,
        isRead: true,
        createdAt: entry.value.createdAt,
        externalId: entry.value.externalId,
        externalUrl: entry.value.externalUrl,
        metadata: entry.value.metadata,
      );
    }

    _cache.clear();
    _cache.addAll(updatedCache);
  }

  /// Get notification by ID
  Future<NotificationModel?> getNotificationById(int id) async {
    return _cache[id];
  }

  /// Delete a notification
  Future<void> deleteNotification(int id) async {
    _cache.remove(id);
  }

  /// Get unread count
  Future<int> getUnreadCount() async {
    return _cache.values.where((n) => !n.isRead).length;
  }

  /// Clear all cached notifications
  Future<void> clearCache() async {
    _cache.clear();
  }
}
