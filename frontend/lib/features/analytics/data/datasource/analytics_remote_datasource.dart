import 'package:notio_app/features/analytics/data/model/weekly_analytics_model.dart';

/// Remote data source for analytics
class AnalyticsRemoteDataSource {
  AnalyticsRemoteDataSource();

  /// Fetch weekly summary analytics
  ///
  /// TODO: Replace with actual API call when backend is ready
  /// Currently returns mock data
  Future<WeeklyAnalyticsModel> fetchWeeklySummary() async {
    // Simulate network delay
    await Future.delayed(const Duration(milliseconds: 800));

    // Get current date and calculate 7 days ago
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    // Create daily trend for last 7 days
    final dailyTrend = <String, int>{};
    for (int i = 6; i >= 0; i--) {
      final date = today.subtract(Duration(days: i));
      final dateStr = date.toIso8601String().split('T')[0];
      // Generate pseudo-random counts based on day
      final count = 5 + (i * 3) % 15;
      dailyTrend[dateStr] = count;
    }

    return WeeklyAnalyticsModel(
      totalNotifications: 68,
      unreadNotifications: 12,
      sourceDistribution: {
        'SLACK': 28,
        'GITHUB': 22,
        'CLAUDE': 12,
        'GMAIL': 6,
      },
      priorityDistribution: {
        'URGENT': 8,
        'HIGH': 18,
        'MEDIUM': 32,
        'LOW': 10,
      },
      dailyTrend: dailyTrend,
      insight: '이번 주는 지난 주 대비 알림이 23% 증가했어요. '
          'Slack에서 가장 많은 알림을 받았으며, '
          '주로 평일 오후 2-4시에 집중되어 있습니다.',
    );
  }
}
