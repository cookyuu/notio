import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Domain entity for weekly analytics
class WeeklyAnalyticsEntity {
  final int totalNotifications;
  final int unreadNotifications;
  final Map<NotificationSource, int> sourceDistribution;
  final Map<NotificationPriority, int> priorityDistribution;
  final Map<DateTime, int> dailyTrend;
  final String insight;

  const WeeklyAnalyticsEntity({
    required this.totalNotifications,
    required this.unreadNotifications,
    required this.sourceDistribution,
    required this.priorityDistribution,
    required this.dailyTrend,
    required this.insight,
  });

  /// Total read notifications
  int get readNotifications => totalNotifications - unreadNotifications;

  /// Read percentage
  double get readPercentage => totalNotifications > 0
      ? (readNotifications / totalNotifications) * 100
      : 0.0;

  /// Most active source
  NotificationSource? get mostActiveSource {
    if (sourceDistribution.isEmpty) return null;

    return sourceDistribution.entries
        .reduce((a, b) => a.value > b.value ? a : b)
        .key;
  }

  /// Highest priority count
  NotificationPriority? get highestPriority {
    if (priorityDistribution.isEmpty) return null;

    return priorityDistribution.entries
        .reduce((a, b) => a.value > b.value ? a : b)
        .key;
  }

  /// Average notifications per day
  double get averagePerDay {
    if (dailyTrend.isEmpty) return 0.0;

    final total = dailyTrend.values.reduce((a, b) => a + b);
    return total / dailyTrend.length;
  }
}
