import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';
import 'package:notio_app/features/chat/domain/repositories/chat_repository.dart';
import 'package:notio_app/features/chat/presentation/providers/chat_state.dart';

/// Chat Notifier
class ChatNotifier extends StateNotifier<ChatState> {
  final ChatRepository _repository;

  ChatNotifier(this._repository) : super(const ChatState()) {
    _loadInitialMessages();
  }

  /// Load initial messages from cache or remote
  Future<void> _loadInitialMessages() async {
    try {
      state = state.copyWith(isLoading: true, error: null);

      try {
        // Try to load from cache first
        final cachedMessages = await _repository.getCachedMessages();
        if (cachedMessages.isNotEmpty) {
          state = state.copyWith(
            messages: cachedMessages,
            isLoading: false,
          );
          return;
        }
      } catch (_) {
        // Ignore cache read failures and continue with remote history.
      }

      // If cache is empty, fetch from remote
      final messages = await _repository.fetchHistory(page: 0, size: 50);
      state = state.copyWith(
        messages: messages,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  /// Send a message (without streaming)
  Future<void> sendMessage(String content) async {
    if (content.trim().isEmpty || state.isSending) return;

    try {
      state = state.copyWith(isSending: true, error: null);

      // Add user message to UI immediately
      final userMessage = ChatMessageEntity(
        id: DateTime.now().millisecondsSinceEpoch,
        role: MessageRole.user,
        content: content.trim(),
        createdAt: DateTime.now(),
      );

      state = state.copyWith(
        messages: [...state.messages, userMessage],
      );

      // Send to repository and get AI response
      final aiMessage = await _repository.sendMessage(content.trim());

      // Add AI response to UI
      state = state.copyWith(
        messages: [...state.messages, aiMessage],
        isSending: false,
      );
    } catch (e) {
      state = state.copyWith(
        isSending: false,
        error: e.toString(),
      );
    }
  }

  /// Send a message with streaming (SSE)
  Future<void> sendMessageWithStreaming(String content) async {
    if (content.trim().isEmpty || state.isSending || state.isStreaming) return;

    try {
      state = state.copyWith(
        isSending: true,
        isStreaming: true,
        error: null,
        streamingContent: '',
      );

      // Add user message to UI immediately
      final userMessage = ChatMessageEntity(
        id: DateTime.now().millisecondsSinceEpoch,
        role: MessageRole.user,
        content: content.trim(),
        createdAt: DateTime.now(),
      );

      state = state.copyWith(
        messages: [...state.messages, userMessage],
        isSending: false,
      );

      // Start streaming
      final stream = _repository.streamMessage(content.trim());
      final buffer = StringBuffer();

      await for (final chunk in stream) {
        buffer.write(chunk);
        state = state.copyWith(
          streamingContent: buffer.toString(),
        );
      }

      // When streaming is complete, create AI message
      final aiMessage = ChatMessageEntity(
        id: DateTime.now().millisecondsSinceEpoch,
        role: MessageRole.assistant,
        content: buffer.toString(),
        createdAt: DateTime.now(),
      );

      // Add to cache
      _repository.addMessageToCache(aiMessage);

      // Update state
      state = state.copyWith(
        messages: [...state.messages, aiMessage],
        isStreaming: false,
        streamingContent: null,
      );
    } catch (e) {
      state = state.copyWith(
        isSending: false,
        isStreaming: false,
        streamingContent: null,
        error: e.toString(),
      );
    }
  }

  /// Refresh messages
  Future<void> refresh() async {
    try {
      state = state.copyWith(isLoading: true, error: null);

      final messages = await _repository.fetchHistory(page: 0, size: 50);
      state = state.copyWith(
        messages: messages,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  /// Clear all messages
  void clearMessages() {
    _repository.clearCache();
    state = const ChatState();
  }
}
