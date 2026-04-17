import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Domain entity for notification detail
class NotificationDetailEntity {
  final int id;
  final int? connectionId;
  final NotificationSource source;
  final String title;
  final String body;
  final NotificationPriority priority;
  final bool isRead;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String? externalId;
  final String? externalUrl;
  final Map<String, dynamic>? metadata;

  const NotificationDetailEntity({
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

  NotificationDetailEntity copyWith({
    int? id,
    int? connectionId,
    NotificationSource? source,
    String? title,
    String? body,
    NotificationPriority? priority,
    bool? isRead,
    DateTime? createdAt,
    DateTime? updatedAt,
    String? externalId,
    String? externalUrl,
    Map<String, dynamic>? metadata,
  }) {
    return NotificationDetailEntity(
      id: id ?? this.id,
      connectionId: connectionId ?? this.connectionId,
      source: source ?? this.source,
      title: title ?? this.title,
      body: body ?? this.body,
      priority: priority ?? this.priority,
      isRead: isRead ?? this.isRead,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      externalId: externalId ?? this.externalId,
      externalUrl: externalUrl ?? this.externalUrl,
      metadata: metadata ?? this.metadata,
    );
  }
}
