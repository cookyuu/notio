import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';

/// Local data source for chat messages (in-memory cache)
/// TODO: Replace with Drift database in Phase 4
class ChatLocalDataSource {
  // In-memory cache for chat messages (최근 50개만 유지)
  final List<ChatMessageEntity> _messages = [];
  static const int maxMessages = 50;

  /// Get cached messages
  List<ChatMessageEntity> getCachedMessages() {
    return List.unmodifiable(_messages);
  }

  /// Cache messages (keeps only the most recent 50)
  void cacheMessages(List<ChatMessageEntity> messages) {
    _messages.clear();

    // Sort by createdAt descending (newest first)
    final sortedMessages = List<ChatMessageEntity>.from(messages)
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));

    // Keep only the most recent maxMessages
    final messagesToCache = sortedMessages.take(maxMessages).toList();

    // Reverse to get chronological order (oldest first)
    _messages.addAll(messagesToCache.reversed);
  }

  /// Add a single message to cache
  void addMessage(ChatMessageEntity message) {
    _messages.add(message);

    // Remove oldest message if exceeds limit
    if (_messages.length > maxMessages) {
      _messages.removeAt(0);
    }
  }

  /// Clear all cached messages
  void clearCache() {
    _messages.clear();
  }

  /// Get message count
  int getMessageCount() {
    return _messages.length;
  }
}
