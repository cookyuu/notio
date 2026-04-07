/// Request DTO for sending chat message
class ChatRequest {
  final String content;

  const ChatRequest({
    required this.content,
  });

  Map<String, dynamic> toJson() {
    return {
      'content': content,
    };
  }
}
