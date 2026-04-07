import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:notio_app/core/database/app_database.dart';
import 'package:notio_app/features/notification/data/model/notification_model.dart';

/// Drift-based local data source for notifications
class NotificationLocalDataSource {
  final AppDatabase _database;

  NotificationLocalDataSource(this._database);

  /// Get all cached notifications, optionally filtered by source
  Future<List<NotificationModel>> getCachedNotifications({
    String? source,
  }) async {
    final notifications = await _database.getAllNotifications(
      source: source,
      limit: 100,
    );

    return notifications.map(_toModel).toList();
  }

  /// Save notifications to cache
  Future<void> cacheNotifications(List<NotificationModel> notifications) async {
    final companions = notifications.map(_toCompanion).toList();
    await _database.insertNotifications(companions);

    // Clean old and expired notifications (TTL: 24h, max: 100)
    await _database.cleanupNotifications();
  }

  /// Mark a notification as read
  Future<void> markAsRead(int notificationId) async {
    await _database.markNotificationAsRead(notificationId);
  }

  /// Mark all notifications as read
  Future<void> markAllAsRead() async {
    await _database.markAllNotificationsAsRead();
  }

  /// Get notification by ID
  Future<NotificationModel?> getNotificationById(int id) async {
    final notification = await _database.getNotificationById(id);
    return notification != null ? _toModel(notification) : null;
  }

  /// Delete a notification
  Future<void> deleteNotification(int id) async {
    await _database.deleteNotification(id);
  }

  /// Get unread count
  Future<int> getUnreadCount() async {
    return await _database.getUnreadCount();
  }

  /// Clear all cached notifications
  Future<void> clearCache() async {
    // Delete all by getting all IDs and deleting them
    final all = await _database.getAllNotifications(limit: 1000);
    for (final notification in all) {
      await _database.deleteNotification(notification.id);
    }
  }

  // ========== Conversion Methods ==========

  /// Convert Drift data to NotificationModel
  NotificationModel _toModel(NotificationTableData data) {
    return NotificationModel(
      id: data.id,
      source: data.source,
      title: data.title,
      body: data.body,
      priority: data.priority,
      isRead: data.isRead,
      createdAt: data.createdAt.toIso8601String(),
      externalId: data.externalId,
      externalUrl: data.externalUrl,
      metadata: data.metadata != null
          ? json.decode(data.metadata!) as Map<String, dynamic>
          : null,
    );
  }

  /// Convert NotificationModel to Drift companion
  NotificationTableCompanion _toCompanion(NotificationModel model) {
    return NotificationTableCompanion(
      id: Value(model.id),
      source: Value(model.source),
      title: Value(model.title),
      body: Value(model.body),
      priority: Value(model.priority),
      isRead: Value(model.isRead),
      createdAt: Value(DateTime.parse(model.createdAt)),
      externalId: Value(model.externalId),
      externalUrl: Value(model.externalUrl),
      metadata: Value(model.metadata != null ? json.encode(model.metadata) : null),
    );
  }
}
