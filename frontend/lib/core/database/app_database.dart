import 'package:drift/drift.dart';
import 'package:notio_app/core/database/tables/notification_table.dart';
import 'connection/connection.dart' as impl;

part 'app_database.g.dart';

@DriftDatabase(tables: [NotificationTable])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(impl.connect());

  @override
  int get schemaVersion => 3;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (Migrator m) async {
        await m.createAll();
        await _createIndexes();
      },
      onUpgrade: (Migrator m, int from, int to) async {
        if (from < 2) {
          await m.createTable(notificationTable);
        }
        if (from < 3) {
          await _createIndexes();
        }
      },
    );
  }

  Future<void> _createIndexes() async {
    await customStatement(
      'CREATE INDEX IF NOT EXISTS idx_notifications_source ON notifications(source)',
    );
    await customStatement(
      'CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC)',
    );
    await customStatement(
      'CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read)',
    );
    await customStatement(
      'CREATE INDEX IF NOT EXISTS idx_notifications_source_created_at ON notifications(source, created_at DESC)',
    );
  }

  // ========== Notification Queries ==========

  /// Get all notifications, optionally filtered by source
  Future<List<NotificationTableData>> getAllNotifications({
    String? source,
    int limit = 100,
  }) {
    final query = select(notificationTable)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)])
      ..limit(limit);

    if (source != null) {
      query.where((tbl) => tbl.source.equals(source));
    }

    return query.get();
  }

  /// Get a single notification by ID
  Future<NotificationTableData?> getNotificationById(int id) {
    return (select(notificationTable)..where((tbl) => tbl.id.equals(id)))
        .getSingleOrNull();
  }

  /// Insert or update a notification
  Future<int> upsertNotification(NotificationTableCompanion notification) {
    return into(notificationTable).insertOnConflictUpdate(notification);
  }

  /// Insert multiple notifications
  Future<void> insertNotifications(
      List<NotificationTableCompanion> notifications) async {
    await batch((batch) {
      batch.insertAllOnConflictUpdate(notificationTable, notifications);
    });
  }

  /// Mark notification as read
  Future<void> markNotificationAsRead(int id) {
    return (update(notificationTable)..where((tbl) => tbl.id.equals(id)))
        .write(const NotificationTableCompanion(isRead: Value(true)));
  }

  /// Mark all notifications as read
  Future<void> markAllNotificationsAsRead() {
    return update(notificationTable)
        .write(const NotificationTableCompanion(isRead: Value(true)));
  }

  /// Delete a notification
  Future<void> deleteNotification(int id) {
    return (delete(notificationTable)..where((tbl) => tbl.id.equals(id))).go();
  }

  /// Delete old notifications (keep only recent 100)
  Future<void> cleanOldNotifications() async {
    final allNotifications = await (select(notificationTable)
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .get();

    if (allNotifications.length > 100) {
      final toDelete = allNotifications.skip(100).map((n) => n.id).toList();
      await (delete(notificationTable)..where((tbl) => tbl.id.isIn(toDelete)))
          .go();
    }
  }

  /// Delete notifications older than 24 hours (TTL-based cleanup)
  Future<int> cleanExpiredNotifications({int ttlHours = 24}) async {
    final expirationDate = DateTime.now().subtract(Duration(hours: ttlHours));
    final query = delete(notificationTable)
      ..where((tbl) => tbl.createdAt.isSmallerThanValue(expirationDate));
    return await query.go();
  }

  /// Comprehensive cleanup: Remove both old and expired notifications
  Future<void> cleanupNotifications({
    int maxCount = 100,
    int ttlHours = 24,
  }) async {
    // First, remove expired notifications
    await cleanExpiredNotifications(ttlHours: ttlHours);
    // Then, limit to max count
    await cleanOldNotifications();
  }

  /// Get unread count
  Future<int> getUnreadCount() async {
    final query = selectOnly(notificationTable)
      ..addColumns([notificationTable.id.count()])
      ..where(notificationTable.isRead.equals(false));

    final result = await query.getSingle();
    return result.read(notificationTable.id.count()) ?? 0;
  }

}
