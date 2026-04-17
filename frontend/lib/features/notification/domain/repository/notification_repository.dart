import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_summary_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';

/// Abstract repository for notifications
abstract class NotificationRepository {
  /// Fetch notification list (summary) with optional source filter and pagination
  Future<List<NotificationSummaryEntity>> fetchNotifications({
    NotificationSource? source,
    int page = 0,
    int size = 20,
  });

  /// Get notification detail by ID
  Future<NotificationDetailEntity> getNotificationDetail(int id);

  /// Get unread notification count
  Future<int> getUnreadCount();

  /// Mark a notification as read
  Future<void> markAsRead(int notificationId);

  /// Mark all notifications as read
  Future<void> markAllAsRead();

  /// Get a single notification by ID (from local cache)
  Future<NotificationEntity?> getNotificationById(int id);

  /// Delete a notification
  Future<void> deleteNotification(int id);

  /// Get cached notifications from local database
  Future<List<NotificationEntity>> getCachedNotifications({
    NotificationSource? source,
  });

  /// Save notifications to local cache
  Future<void> cacheNotifications(List<NotificationEntity> notifications);

  /// Clear all cached notifications
  Future<void> clearCache();
}
