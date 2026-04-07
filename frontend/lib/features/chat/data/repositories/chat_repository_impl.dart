import 'package:notio_app/features/chat/data/datasources/chat_local_datasource.dart';
import 'package:notio_app/features/chat/data/datasources/chat_remote_datasource.dart';
import 'package:notio_app/features/chat/data/models/chat_message_model.dart';
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
      final userModel = ChatMessageModel.fromEntity(userMessage);
      await _localDataSource.addMessage(userModel);

      // Send to remote and get AI response
      final request = ChatRequest(content: content);
      final responseModel = await _remoteDataSource.sendMessage(request);
      final aiMessage = responseModel.toEntity();

      // Add AI response to cache
      await _localDataSource.addMessage(responseModel);

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
        await _localDataSource.cacheMessages(models);
      }

      return entities;
    } catch (e) {
      // On error, return from cache if available
      if (page == 0) {
        final cachedModels = await _localDataSource.getCachedMessages();
        return cachedModels.map((model) => model.toEntity()).toList();
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
    // Note: This is now async, but interface expects sync
    // We'll need to update this in the interface later
    throw UnimplementedError('Use fetchHistory instead');
  }

  @override
  void cacheMessages(List<ChatMessageEntity> messages) {
    final models = messages.map(ChatMessageModel.fromEntity).toList();
    _localDataSource.cacheMessages(models);
  }

  @override
  void addMessageToCache(ChatMessageEntity message) {
    final model = ChatMessageModel.fromEntity(message);
    _localDataSource.addMessage(model);
  }

  @override
  void clearCache() {
    _localDataSource.clearCache();
  }
}
