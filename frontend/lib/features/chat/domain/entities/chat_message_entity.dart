import 'package:notio_app/features/chat/domain/entities/message_role.dart';

/// Domain entity for chat messages
class ChatMessageEntity {
  final int id;
  final MessageRole role;
  final String content;
  final DateTime createdAt;

  const ChatMessageEntity({
    required this.id,
    required this.role,
    required this.content,
    required this.createdAt,
  });

  ChatMessageEntity copyWith({
    int? id,
    MessageRole? role,
    String? content,
    DateTime? createdAt,
  }) {
    return ChatMessageEntity(
      id: id ?? this.id,
      role: role ?? this.role,
      content: content ?? this.content,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChatMessageEntity &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          role == other.role &&
          content == other.content &&
          createdAt == other.createdAt;

  @override
  int get hashCode =>
      id.hashCode ^ role.hashCode ^ content.hashCode ^ createdAt.hashCode;
}
