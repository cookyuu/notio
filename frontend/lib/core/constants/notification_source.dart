/// Notification source enum
enum NotificationSource {
  claude,
  slack,
  github,
  gmail,
}

extension NotificationSourceExtension on NotificationSource {
  String get displayName {
    switch (this) {
      case NotificationSource.claude:
        return 'Claude';
      case NotificationSource.slack:
        return 'Slack';
      case NotificationSource.github:
        return 'GitHub';
      case NotificationSource.gmail:
        return 'Gmail';
    }
  }

  String get apiValue {
    switch (this) {
      case NotificationSource.claude:
        return 'CLAUDE';
      case NotificationSource.slack:
        return 'SLACK';
      case NotificationSource.github:
        return 'GITHUB';
      case NotificationSource.gmail:
        return 'GMAIL';
    }
  }

  static NotificationSource fromApiValue(String value) {
    switch (value.toUpperCase()) {
      case 'CLAUDE':
        return NotificationSource.claude;
      case 'SLACK':
        return NotificationSource.slack;
      case 'GITHUB':
        return NotificationSource.github;
      case 'GMAIL':
        return NotificationSource.gmail;
      default:
        throw ArgumentError('Invalid notification source: $value');
    }
  }
}
