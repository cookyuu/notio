import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/features/notification/domain/entity/notification_summary_entity.dart';

/// Data model for notification list (summary, DTO)
class NotificationSummaryModel {
  final int id;
  final String source;
  final String title;
  final String bodyPreview;
  final String priority;
  final bool isRead;
  final String createdAt;

  const NotificationSummaryModel({
    required this.id,
    required this.source,
    required this.title,
    required this.bodyPreview,
    required this.priority,
    required this.isRead,
    required this.createdAt,
  });

  factory NotificationSummaryModel.fromJson(Map<String, dynamic> json) {
    return NotificationSummaryModel(
      id: json['id'] as int,
      source: json['source'] as String,
      title: json['title'] as String,
      bodyPreview: json['body_preview'] as String,
      priority: json['priority'] as String,
      isRead: json['is_read'] as bool,
      createdAt: json['created_at'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'source': source,
      'title': title,
      'body_preview': bodyPreview,
      'priority': priority,
      'is_read': isRead,
      'created_at': createdAt,
    };
  }

  /// Convert to domain entity
  NotificationSummaryEntity toEntity() {
    return NotificationSummaryEntity(
      id: id,
      source: NotificationSourceExtension.fromApiValue(source),
      title: title,
      bodyPreview: bodyPreview,
      priority: NotificationPriorityExtension.fromApiValue(priority),
      isRead: isRead,
      createdAt: DateTime.parse(createdAt),
    );
  }

  /// Create from domain entity
  factory NotificationSummaryModel.fromEntity(NotificationSummaryEntity entity) {
    return NotificationSummaryModel(
      id: entity.id,
      source: entity.source.apiValue,
      title: entity.title,
      bodyPreview: entity.bodyPreview,
      priority: entity.priority.apiValue,
      isRead: entity.isRead,
      createdAt: entity.createdAt.toIso8601String(),
    );
  }
}
