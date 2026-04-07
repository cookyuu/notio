import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';
import 'package:notio_app/features/analytics/presentation/providers/analytics_providers.dart';
import 'package:notio_app/features/analytics/presentation/widgets/daily_trend_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/insight_card.dart';
import 'package:notio_app/features/analytics/presentation/widgets/priority_distribution_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/source_distribution_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/weekly_stats_card.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

/// Analytics screen (Phase 3)
class AnalyticsScreen extends ConsumerWidget {
  const AnalyticsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final analyticsAsync = ref.watch(weeklyAnalyticsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Analytics'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.read(refreshAnalyticsProvider)();
            },
            tooltip: '새로고침',
          ),
        ],
      ),
      body: analyticsAsync.when(
        data: (analytics) => RefreshIndicator(
          onRefresh: () async {
            ref.read(refreshAnalyticsProvider)();
            // Wait for the new data to load
            await ref.read(weeklyAnalyticsProvider.future);
          },
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(AppSpacing.s16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Weekly Stats Card
                WeeklyStatsCard(
                  totalNotifications: analytics.totalNotifications,
                  unreadNotifications: analytics.unreadNotifications,
                  readPercentage: analytics.readPercentage,
                ),
                const SizedBox(height: AppSpacing.s16),

                // Source Distribution Chart
                SourceDistributionChart(
                  distribution: analytics.sourceDistribution,
                ),
                const SizedBox(height: AppSpacing.s16),

                // Priority Distribution Chart
                PriorityDistributionChart(
                  distribution: analytics.priorityDistribution,
                ),
                const SizedBox(height: AppSpacing.s16),

                // Daily Trend Chart
                DailyTrendChart(
                  dailyTrend: analytics.dailyTrend,
                ),
                const SizedBox(height: AppSpacing.s16),

                // AI Insight Card
                InsightCard(
                  insight: analytics.insight,
                ),
                const SizedBox(height: AppSpacing.s16),

                // Additional stats
                _buildAdditionalStats(analytics),
              ],
            ),
          ),
        ),
        loading: () => const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(
                color: AppColors.primary,
              ),
              SizedBox(height: AppSpacing.s16),
              Text(
                '분석 데이터를 불러오는 중...',
                style: AppTextStyles.bodyMedium,
              ),
            ],
          ),
        ),
        error: (error, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.error_outline,
                color: AppColors.error,
                size: 64,
              ),
              const SizedBox(height: AppSpacing.s16),
              Text(
                '데이터를 불러올 수 없습니다',
                style: AppTextStyles.headlineSmall.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: AppSpacing.s8),
              Text(
                error.toString(),
                style: AppTextStyles.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: AppSpacing.s16),
              ElevatedButton.icon(
                onPressed: () {
                  ref.read(refreshAnalyticsProvider)();
                },
                icon: const Icon(Icons.refresh),
                label: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAdditionalStats(WeeklyAnalyticsEntity analytics) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.s16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(
          color: AppColors.divider,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '추가 통계',
            style: AppTextStyles.headlineSmall.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: AppSpacing.s12),
          _buildStatRow(
            '일평균 알림',
            '${analytics.averagePerDay.toStringAsFixed(1)}개',
          ),
          const SizedBox(height: AppSpacing.s8),
          if (analytics.mostActiveSource != null)
            _buildStatRow(
              '가장 활발한 소스',
              analytics.mostActiveSource!.displayName,
            ),
          const SizedBox(height: AppSpacing.s8),
          if (analytics.highestPriority != null)
            _buildStatRow(
              '가장 많은 우선순위',
              analytics.highestPriority!.displayName,
            ),
        ],
      ),
    );
  }

  Widget _buildStatRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        Text(
          value,
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}
