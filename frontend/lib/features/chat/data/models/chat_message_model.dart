import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';

/// Data model for chat messages (DTO)
class ChatMessageModel {
  final int id;
  final String role;
  final String content;
  final String createdAt;

  const ChatMessageModel({
    required this.id,
    required this.role,
    required this.content,
    required this.createdAt,
  });

  factory ChatMessageModel.fromJson(Map<String, dynamic> json) {
    return ChatMessageModel(
      id: json['id'] as int,
      role: json['role'] as String,
      content: json['content'] as String,
      createdAt: json['created_at'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'role': role,
      'content': content,
      'created_at': createdAt,
    };
  }

  /// Convert to domain entity
  ChatMessageEntity toEntity() {
    return ChatMessageEntity(
      id: id,
      role: MessageRole.fromApiValue(role),
      content: content,
      createdAt: DateTime.parse(createdAt),
    );
  }

  /// Create from domain entity
  factory ChatMessageModel.fromEntity(ChatMessageEntity entity) {
    return ChatMessageModel(
      id: entity.id,
      role: entity.role.apiValue,
      content: entity.content,
      createdAt: entity.createdAt.toIso8601String(),
    );
  }
}
