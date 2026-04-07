import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';

/// Abstract repository for analytics
abstract class AnalyticsRepository {
  /// Fetch weekly summary analytics
  ///
  /// Returns analytics data for the past 7 days including:
  /// - Total and unread notification counts
  /// - Source distribution
  /// - Priority distribution
  /// - Daily trend
  /// - AI-generated insight
  Future<WeeklyAnalyticsEntity> fetchWeeklySummary();
}
