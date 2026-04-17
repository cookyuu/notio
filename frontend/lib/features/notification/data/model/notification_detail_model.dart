import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Data model for notification detail (DTO)
class NotificationDetailModel {
  final int id;
  final int? connectionId;
  final String source;
  final String title;
  final String body;
  final String priority;
  final bool isRead;
  final String createdAt;
  final String updatedAt;
  final String? externalId;
  final String? externalUrl;
  final Map<String, dynamic>? metadata;

  const NotificationDetailModel({
    required this.id,
    required this.source,
    required this.title,
    required this.body,
    required this.priority,
    required this.isRead,
    required this.createdAt,
    required this.updatedAt,
    this.connectionId,
    this.externalId,
    this.externalUrl,
    this.metadata,
  });

  factory NotificationDetailModel.fromJson(Map<String, dynamic> json) {
    return NotificationDetailModel(
      id: json['id'] as int,
      connectionId: json['connection_id'] as int?,
      source: json['source'] as String,
      title: json['title'] as String,
      body: json['body'] as String,
      priority: json['priority'] as String,
      isRead: json['is_read'] as bool,
      createdAt: json['created_at'] as String,
      updatedAt: json['updated_at'] as String,
      externalId: json['external_id'] as String?,
      externalUrl: json['external_url'] as String?,
      metadata: (json['metadata'] as Map?)?.cast<String, dynamic>(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'connection_id': connectionId,
      'source': source,
      'title': title,
      'body': body,
      'priority': priority,
      'is_read': isRead,
      'created_at': createdAt,
      'updated_at': updatedAt,
      'external_id': externalId,
      'external_url': externalUrl,
      'metadata': metadata,
    };
  }

  /// Convert to domain entity
  NotificationDetailEntity toEntity() {
    return NotificationDetailEntity(
      id: id,
      connectionId: connectionId,
      source: NotificationSourceExtension.fromApiValue(source),
      title: title,
      body: body,
      priority: NotificationPriorityExtension.fromApiValue(priority),
      isRead: isRead,
      createdAt: DateTime.parse(createdAt),
      updatedAt: DateTime.parse(updatedAt),
      externalId: externalId,
      externalUrl: externalUrl,
      metadata: metadata,
    );
  }

  /// Create from domain entity
  factory NotificationDetailModel.fromEntity(NotificationDetailEntity entity) {
    return NotificationDetailModel(
      id: entity.id,
      connectionId: entity.connectionId,
      source: entity.source.apiValue,
      title: entity.title,
      body: entity.body,
      priority: entity.priority.apiValue,
      isRead: entity.isRead,
      createdAt: entity.createdAt.toIso8601String(),
      updatedAt: entity.updatedAt.toIso8601String(),
      externalId: entity.externalId,
      externalUrl: entity.externalUrl,
      metadata: entity.metadata,
    );
  }
}
