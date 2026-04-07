import 'package:notio_app/features/chat/data/models/chat_message_model.dart';

/// Response DTO for chat message
class ChatResponse {
  final ChatMessageModel message;

  const ChatResponse({
    required this.message,
  });

  factory ChatResponse.fromJson(Map<String, dynamic> json) {
    return ChatResponse(
      message: ChatMessageModel.fromJson(json['message'] as Map<String, dynamic>),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'message': message.toJson(),
    };
  }
}
