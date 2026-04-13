import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';
import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';

/// Abstract repository for chat messages
abstract class ChatRepository {
  /// Send a message and get response
  Future<ChatMessageEntity> sendMessage(String content);

  /// Stream a message response (SSE)
  Stream<String> streamMessage(String content);

  /// Fetch chat history with pagination
  Future<List<ChatMessageEntity>> fetchHistory({
    int page = 0,
    int size = 20,
  });

  /// Get daily summary
  Future<DailySummaryModel> getDailySummary();

  /// Get cached messages from local storage
  Future<List<ChatMessageEntity>> getCachedMessages();

  /// Cache messages to local storage
  void cacheMessages(List<ChatMessageEntity> messages);

  /// Add a message to cache
  void addMessageToCache(ChatMessageEntity message);

  /// Clear all cached messages
  void clearCache();
}
