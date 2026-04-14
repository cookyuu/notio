import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Data model for notifications (DTO)
class NotificationModel {
  final int id;
  final String source;
  final String title;
  final String body;
  final String priority;
  final bool isRead;
  final String createdAt;
  final String? externalId;
  final String? externalUrl;
  final Map<String, dynamic>? metadata;

  const NotificationModel({
    required this.id,
    required this.source,
    required this.title,
    required this.body,
    required this.priority,
    required this.isRead,
    required this.createdAt,
    this.externalId,
    this.externalUrl,
    this.metadata,
  });

  factory NotificationModel.fromJson(Map<String, dynamic> json) {
    return NotificationModel(
      id: json['id'] as int,
      source: json['source'] as String,
      title: json['title'] as String,
      body: json['body'] as String,
      priority: json['priority'] as String,
      isRead: json['is_read'] as bool,
      createdAt: json['created_at'] as String,
      externalId: json['external_id'] as String?,
      externalUrl: json['external_url'] as String?,
      metadata: (json['metadata'] as Map?)?.cast<String, dynamic>(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'source': source,
      'title': title,
      'body': body,
      'priority': priority,
      'is_read': isRead,
      'created_at': createdAt,
      'external_id': externalId,
      'external_url': externalUrl,
      'metadata': metadata,
    };
  }

  /// Convert to domain entity
  NotificationEntity toEntity() {
    return NotificationEntity(
      id: id,
      source: NotificationSourceExtension.fromApiValue(source),
      title: title,
      body: body,
      priority: NotificationPriorityExtension.fromApiValue(priority),
      isRead: isRead,
      createdAt: DateTime.parse(createdAt),
      externalId: externalId,
      externalUrl: externalUrl,
      metadata: metadata,
    );
  }

  /// Create from domain entity
  factory NotificationModel.fromEntity(NotificationEntity entity) {
    return NotificationModel(
      id: entity.id,
      source: entity.source.apiValue,
      title: entity.title,
      body: entity.body,
      priority: entity.priority.apiValue,
      isRead: entity.isRead,
      createdAt: entity.createdAt.toIso8601String(),
      externalId: entity.externalId,
      externalUrl: entity.externalUrl,
      metadata: entity.metadata,
    );
  }
}
