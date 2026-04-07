/// Message role enum for chat messages
enum MessageRole {
  user,
  assistant;

  String get apiValue {
    switch (this) {
      case MessageRole.user:
        return 'user';
      case MessageRole.assistant:
        return 'assistant';
    }
  }

  static MessageRole fromApiValue(String value) {
    switch (value) {
      case 'user':
        return MessageRole.user;
      case 'assistant':
        return MessageRole.assistant;
      default:
        throw ArgumentError('Unknown message role: $value');
    }
  }
}
