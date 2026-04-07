import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';

/// Chat state
class ChatState {
  final List<ChatMessageEntity> messages;
  final bool isLoading;
  final bool isSending;
  final bool isStreaming;
  final String? error;
  final String? streamingContent;

  const ChatState({
    this.messages = const [],
    this.isLoading = false,
    this.isSending = false,
    this.isStreaming = false,
    this.error,
    this.streamingContent,
  });

  ChatState copyWith({
    List<ChatMessageEntity>? messages,
    bool? isLoading,
    bool? isSending,
    bool? isStreaming,
    String? error,
    String? streamingContent,
  }) {
    return ChatState(
      messages: messages ?? this.messages,
      isLoading: isLoading ?? this.isLoading,
      isSending: isSending ?? this.isSending,
      isStreaming: isStreaming ?? this.isStreaming,
      error: error,
      streamingContent: streamingContent,
    );
  }
}
