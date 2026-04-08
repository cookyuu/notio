import 'dart:convert';

/// Payload for local notification deep linking
class NotificationPayload {
  final String route;
  final int? notificationId;
  final Map<String, dynamic>? extraData;

  const NotificationPayload({
    required this.route,
    this.notificationId,
    this.extraData,
  });

  /// Convert to JSON string
  String toJson() {
    return json.encode({
      'route': route,
      if (notificationId != null) 'notificationId': notificationId,
      if (extraData != null) 'extraData': extraData,
    });
  }

  /// Parse from JSON string
  static NotificationPayload? fromJson(String? jsonString) {
    if (jsonString == null || jsonString.isEmpty) return null;

    try {
      final Map<String, dynamic> data = json.decode(jsonString);
      return NotificationPayload(
        route: data['route'] as String,
        notificationId: data['notificationId'] as int?,
        extraData: data['extraData'] as Map<String, dynamic>?,
      );
    } catch (e) {
      // If parsing fails, return null
      return null;
    }
  }

  /// Create payload for notification detail
  factory NotificationPayload.notificationDetail(int notificationId) {
    return NotificationPayload(
      route: '/notifications',
      notificationId: notificationId,
    );
  }

  /// Create payload for chat
  factory NotificationPayload.chat() {
    return const NotificationPayload(route: '/chat');
  }

  /// Create payload for analytics
  factory NotificationPayload.analytics() {
    return const NotificationPayload(route: '/analytics');
  }
}
