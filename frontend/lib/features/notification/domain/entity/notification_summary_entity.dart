import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Domain entity for notification list (summary)
class NotificationSummaryEntity {
  final int id;
  final NotificationSource source;
  final String title;
  final String bodyPreview;
  final NotificationPriority priority;
  final bool isRead;
  final DateTime createdAt;

  const NotificationSummaryEntity({
    required this.id,
    required this.source,
    required this.title,
    required this.bodyPreview,
    required this.priority,
    required this.isRead,
    required this.createdAt,
  });

  NotificationSummaryEntity copyWith({
    int? id,
    NotificationSource? source,
    String? title,
    String? bodyPreview,
    NotificationPriority? priority,
    bool? isRead,
    DateTime? createdAt,
  }) {
    return NotificationSummaryEntity(
      id: id ?? this.id,
      source: source ?? this.source,
      title: title ?? this.title,
      bodyPreview: bodyPreview ?? this.bodyPreview,
      priority: priority ?? this.priority,
      isRead: isRead ?? this.isRead,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}
