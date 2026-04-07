import 'package:drift/drift.dart';
import 'package:notio_app/core/database/app_database.dart';
import 'package:notio_app/features/chat/data/models/chat_message_model.dart';

/// Drift-based local data source for chat messages
class ChatLocalDataSource {
  final AppDatabase _database;

  ChatLocalDataSource(this._database);

  /// Get all cached chat messages
  Future<List<ChatMessageModel>> getCachedMessages({int limit = 50}) async {
    final messages = await _database.getAllChatMessages(limit: limit);
    return messages.map(_toModel).toList();
  }

  /// Cache messages (keeps only the most recent 50)
  Future<void> cacheMessages(List<ChatMessageModel> messages) async {
    // Clear existing messages
    await _database.deleteAllChatMessages();

    // Sort by createdAt descending (newest first)
    final sortedMessages = List<ChatMessageModel>.from(messages)
      ..sort((a, b) => DateTime.parse(b.createdAt).compareTo(DateTime.parse(a.createdAt)));

    // Keep only the most recent 50
    final messagesToCache = sortedMessages.take(50).toList();

    // Convert to companions and insert
    final companions = messagesToCache.map(_toCompanion).toList();
    await _database.insertChatMessages(companions);
  }

  /// Add a single message to cache
  Future<void> addMessage(ChatMessageModel message) async {
    final companion = _toCompanion(message);
    await _database.insertChatMessage(companion);

    // Clean old messages (keep only recent 50)
    await _database.cleanOldChatMessages();
  }

  /// Clear all cached messages
  Future<void> clearCache() async {
    await _database.deleteAllChatMessages();
  }

  /// Get message count
  Future<int> getMessageCount() async {
    final messages = await _database.getAllChatMessages(limit: 1000);
    return messages.length;
  }

  // ========== Conversion Methods ==========

  /// Convert Drift data to ChatMessageModel
  ChatMessageModel _toModel(ChatMessageTableData data) {
    return ChatMessageModel(
      id: data.id,
      role: data.role,
      content: data.content,
      createdAt: data.createdAt.toIso8601String(),
    );
  }

  /// Convert ChatMessageModel to Drift companion
  ChatMessageTableCompanion _toCompanion(ChatMessageModel model) {
    return ChatMessageTableCompanion(
      id: Value(model.id),
      role: Value(model.role),
      content: Value(model.content),
      createdAt: Value(DateTime.parse(model.createdAt)),
    );
  }
}
