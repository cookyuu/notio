import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Domain entity for notifications
class NotificationEntity {
  final int id;
  final NotificationSource source;
  final String title;
  final String body;
  final NotificationPriority priority;
  final bool isRead;
  final DateTime createdAt;
  final String? externalId;
  final String? externalUrl;
  final Map<String, dynamic>? metadata;

  const NotificationEntity({
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

  NotificationEntity copyWith({
    int? id,
    NotificationSource? source,
    String? title,
    String? body,
    NotificationPriority? priority,
    bool? isRead,
    DateTime? createdAt,
    String? externalId,
    String? externalUrl,
    Map<String, dynamic>? metadata,
  }) {
    return NotificationEntity(
      id: id ?? this.id,
      source: source ?? this.source,
      title: title ?? this.title,
      body: body ?? this.body,
      priority: priority ?? this.priority,
      isRead: isRead ?? this.isRead,
      createdAt: createdAt ?? this.createdAt,
      externalId: externalId ?? this.externalId,
      externalUrl: externalUrl ?? this.externalUrl,
      metadata: metadata ?? this.metadata,
    );
  }
}
