import 'package:drift/drift.dart';
import 'package:notio_app/core/database/tables/notification_table.dart';
import 'package:notio_app/core/database/tables/chat_message_table.dart';
import 'connection/connection.dart' as impl;

part 'app_database.g.dart';

/// Main database class for the app
@DriftDatabase(tables: [NotificationTable, ChatMessageTable])
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
          // Migration from version 1 to 2
          await m.createTable(notificationTable);
          await m.createTable(chatMessageTable);
        }
        if (from < 3) {
          // Migration from version 2 to 3: Add indexes
          await _createIndexes();
        }
      },
    );
  }

  /// Create indexes for performance optimization
  Future<void> _createIndexes() async {
    // Notification indexes
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

    // Chat message indexes
    await customStatement(
      'CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at ASC)',
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

  // ========== Chat Message Queries ==========

  /// Get all chat messages
  Future<List<ChatMessageTableData>> getAllChatMessages({int limit = 50}) {
    return (select(chatMessageTable)
          ..orderBy([(t) => OrderingTerm.asc(t.createdAt)])
          ..limit(limit))
        .get();
  }

  /// Insert a chat message
  Future<int> insertChatMessage(ChatMessageTableCompanion message) {
    return into(chatMessageTable).insert(message);
  }

  /// Insert multiple chat messages
  Future<void> insertChatMessages(
      List<ChatMessageTableCompanion> messages) async {
    if (messages.isEmpty) {
      return;
    }

    await batch((batch) {
      batch.insertAll(chatMessageTable, messages);
    });
  }

  /// Delete all chat messages
  Future<void> deleteAllChatMessages() {
    return delete(chatMessageTable).go();
  }

  /// Clean old chat messages (keep only recent 50)
  Future<void> cleanOldChatMessages() async {
    final allMessages = await (select(chatMessageTable)
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .get();

    if (allMessages.length > 50) {
      final toDelete = allMessages.skip(50).map((m) => m.id).toList();
      await (delete(chatMessageTable)..where((tbl) => tbl.id.isIn(toDelete)))
          .go();
    }
  }

  /// Delete chat messages older than specified hours (TTL-based cleanup)
  Future<int> cleanExpiredChatMessages({int ttlHours = 72}) async {
    final expirationDate = DateTime.now().subtract(Duration(hours: ttlHours));
    final query = delete(chatMessageTable)
      ..where((tbl) => tbl.createdAt.isSmallerThanValue(expirationDate));
    return await query.go();
  }

  /// Comprehensive cleanup: Remove both old and expired chat messages
  Future<void> cleanupChatMessages({
    int maxCount = 50,
    int ttlHours = 72,
  }) async {
    // First, remove expired messages
    await cleanExpiredChatMessages(ttlHours: ttlHours);
    // Then, limit to max count
    await cleanOldChatMessages();
  }
}
