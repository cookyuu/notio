import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';

/// Abstract repository for analytics
abstract class AnalyticsRepository {
  /// Fetch weekly summary analytics
  Future<WeeklyAnalyticsEntity> fetchWeeklySummary();

  /// Fetch AI token usage analytics
  Future<AiUsageEntity> fetchAiUsage(AiUsageFilter filter);
}
