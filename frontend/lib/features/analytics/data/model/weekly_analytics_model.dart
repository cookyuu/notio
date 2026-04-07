import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Data model for weekly analytics (DTO)
class WeeklyAnalyticsModel {
  final int totalNotifications;
  final int unreadNotifications;
  final Map<String, int> sourceDistribution;
  final Map<String, int> priorityDistribution;
  final Map<String, int> dailyTrend;
  final String insight;

  const WeeklyAnalyticsModel({
    required this.totalNotifications,
    required this.unreadNotifications,
    required this.sourceDistribution,
    required this.priorityDistribution,
    required this.dailyTrend,
    required this.insight,
  });

  factory WeeklyAnalyticsModel.fromJson(Map<String, dynamic> json) {
    return WeeklyAnalyticsModel(
      totalNotifications: json['total_notifications'] as int,
      unreadNotifications: json['unread_notifications'] as int,
      sourceDistribution: Map<String, int>.from(json['source_distribution'] as Map),
      priorityDistribution: Map<String, int>.from(json['priority_distribution'] as Map),
      dailyTrend: Map<String, int>.from(json['daily_trend'] as Map),
      insight: json['insight'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'total_notifications': totalNotifications,
      'unread_notifications': unreadNotifications,
      'source_distribution': sourceDistribution,
      'priority_distribution': priorityDistribution,
      'daily_trend': dailyTrend,
      'insight': insight,
    };
  }

  /// Convert to domain entity
  WeeklyAnalyticsEntity toEntity() {
    // Convert source distribution
    final sourceMap = <NotificationSource, int>{};
    sourceDistribution.forEach((key, value) {
      final source = NotificationSourceExtension.fromApiValue(key);
      sourceMap[source] = value;
    });

    // Convert priority distribution
    final priorityMap = <NotificationPriority, int>{};
    priorityDistribution.forEach((key, value) {
      final priority = NotificationPriorityExtension.fromApiValue(key);
      priorityMap[priority] = value;
    });

    // Convert daily trend (ISO date string to DateTime)
    final trendMap = <DateTime, int>{};
    dailyTrend.forEach((key, value) {
      final date = DateTime.parse(key);
      trendMap[date] = value;
    });

    return WeeklyAnalyticsEntity(
      totalNotifications: totalNotifications,
      unreadNotifications: unreadNotifications,
      sourceDistribution: sourceMap,
      priorityDistribution: priorityMap,
      dailyTrend: trendMap,
      insight: insight,
    );
  }

  /// Create from domain entity
  factory WeeklyAnalyticsModel.fromEntity(WeeklyAnalyticsEntity entity) {
    // Convert source distribution
    final sourceMap = <String, int>{};
    entity.sourceDistribution.forEach((key, value) {
      sourceMap[key.apiValue] = value;
    });

    // Convert priority distribution
    final priorityMap = <String, int>{};
    entity.priorityDistribution.forEach((key, value) {
      priorityMap[key.apiValue] = value;
    });

    // Convert daily trend (DateTime to ISO date string)
    final trendMap = <String, int>{};
    entity.dailyTrend.forEach((key, value) {
      trendMap[key.toIso8601String().split('T')[0]] = value;
    });

    return WeeklyAnalyticsModel(
      totalNotifications: entity.totalNotifications,
      unreadNotifications: entity.unreadNotifications,
      sourceDistribution: sourceMap,
      priorityDistribution: priorityMap,
      dailyTrend: trendMap,
      insight: entity.insight,
    );
  }
}
