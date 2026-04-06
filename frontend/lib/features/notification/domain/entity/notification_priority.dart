/// Notification priority levels
enum NotificationPriority {
  low,
  medium,
  high,
  urgent,
}

extension NotificationPriorityExtension on NotificationPriority {
  String get displayName {
    switch (this) {
      case NotificationPriority.low:
        return '낮음';
      case NotificationPriority.medium:
        return '보통';
      case NotificationPriority.high:
        return '높음';
      case NotificationPriority.urgent:
        return '긴급';
    }
  }

  String get apiValue {
    switch (this) {
      case NotificationPriority.low:
        return 'LOW';
      case NotificationPriority.medium:
        return 'MEDIUM';
      case NotificationPriority.high:
        return 'HIGH';
      case NotificationPriority.urgent:
        return 'URGENT';
    }
  }

  static NotificationPriority fromApiValue(String value) {
    switch (value.toUpperCase()) {
      case 'LOW':
        return NotificationPriority.low;
      case 'MEDIUM':
        return NotificationPriority.medium;
      case 'HIGH':
        return NotificationPriority.high;
      case 'URGENT':
        return NotificationPriority.urgent;
      default:
        return NotificationPriority.medium;
    }
  }
}
