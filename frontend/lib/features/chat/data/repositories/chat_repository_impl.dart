import 'package:notio_app/features/chat/data/datasources/chat_local_datasource.dart';
import 'package:notio_app/features/chat/data/datasources/chat_remote_datasource.dart';
import 'package:notio_app/features/chat/data/models/chat_request.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';
import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';
import 'package:notio_app/features/chat/domain/repositories/chat_repository.dart';

/// Implementation of ChatRepository
class ChatRepositoryImpl implements ChatRepository {
  final ChatRemoteDataSource _remoteDataSource;
  final ChatLocalDataSource _localDataSource;

  ChatRepositoryImpl({
    required ChatRemoteDataSource remoteDataSource,
    required ChatLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

  @override
  Future<ChatMessageEntity> sendMessage(String content) async {
    try {
      // Create user message
      final userMessage = ChatMessageEntity(
        id: DateTime.now().millisecondsSinceEpoch,
        role: MessageRole.user,
        content: content,
        createdAt: DateTime.now(),
      );

      // Add user message to cache
      _localDataSource.addMessage(userMessage);

      // Send to remote and get AI response
      final request = ChatRequest(content: content);
      final responseModel = await _remoteDataSource.sendMessage(request);
      final aiMessage = responseModel.toEntity();

      // Add AI response to cache
      _localDataSource.addMessage(aiMessage);

      return aiMessage;
    } catch (e) {
      // On error, try to return from cache if available
      rethrow;
    }
  }

  @override
  Stream<String> streamMessage(String content) {
    // Note: This returns the streaming chunks, not the full message
    // The caller should accumulate the chunks and create the message entity
    final request = ChatRequest(content: content);
    return _remoteDataSource.streamMessage(request);
  }

  @override
  Future<List<ChatMessageEntity>> fetchHistory({
    int page = 0,
    int size = 20,
  }) async {
    try {
      // Try to fetch from remote
      final models = await _remoteDataSource.fetchHistory(page: page, size: size);
      final entities = models.map((model) => model.toEntity()).toList();

      // Cache the results on first page
      if (page == 0) {
        _localDataSource.cacheMessages(entities);
      }

      return entities;
    } catch (e) {
      // On error, return from cache if available
      if (page == 0) {
        return _localDataSource.getCachedMessages();
      }
      rethrow;
    }
  }

  @override
  Future<DailySummaryModel> getDailySummary() async {
    // TODO: Implement caching with 24-hour TTL in Phase 3
    return await _remoteDataSource.getDailySummary();
  }

  @override
  List<ChatMessageEntity> getCachedMessages() {
    return _localDataSource.getCachedMessages();
  }

  @override
  void cacheMessages(List<ChatMessageEntity> messages) {
    _localDataSource.cacheMessages(messages);
  }

  @override
  void addMessageToCache(ChatMessageEntity message) {
    _localDataSource.addMessage(message);
  }

  @override
  void clearCache() {
    _localDataSource.clearCache();
  }
}
