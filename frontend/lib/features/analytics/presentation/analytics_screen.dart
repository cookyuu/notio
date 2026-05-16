import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/features/analytics/presentation/providers/analytics_providers.dart';
import 'package:notio_app/features/analytics/presentation/widgets/ai_usage_summary_card.dart';
import 'package:notio_app/features/analytics/presentation/widgets/daily_trend_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/insight_card.dart';
import 'package:notio_app/features/analytics/presentation/widgets/model_distribution_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/priority_distribution_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/source_distribution_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/token_trend_chart.dart';
import 'package:notio_app/features/analytics/presentation/widgets/weekly_stats_card.dart';

class AnalyticsScreen extends ConsumerStatefulWidget {
  const AnalyticsScreen({super.key});

  @override
  ConsumerState<AnalyticsScreen> createState() => _AnalyticsScreenState();
}

class _AnalyticsScreenState extends ConsumerState<AnalyticsScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Analytics'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.read(refreshAnalyticsProvider)(),
            tooltip: '새로고침',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '알림 통계'),
            Tab(text: 'AI 토큰'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [
          _NotificationAnalyticsTab(),
          _AiUsageTab(),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Tab 0: Notification Analytics (extracted from the original screen)
// ---------------------------------------------------------------------------

class _NotificationAnalyticsTab extends ConsumerWidget {
  const _NotificationAnalyticsTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final analyticsAsync = ref.watch(weeklyAnalyticsProvider);

    return analyticsAsync.when(
      data: (analytics) => RefreshIndicator(
        onRefresh: () async {
          ref.read(refreshAnalyticsProvider)();
          await ref.read(weeklyAnalyticsProvider.future);
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              WeeklyStatsCard(
                totalNotifications: analytics.totalNotifications,
                unreadNotifications: analytics.unreadNotifications,
                readPercentage: analytics.readPercentage,
              ),
              const SizedBox(height: AppSpacing.s16),
              SourceDistributionChart(distribution: analytics.sourceDistribution),
              const SizedBox(height: AppSpacing.s16),
              PriorityDistributionChart(distribution: analytics.priorityDistribution),
              const SizedBox(height: AppSpacing.s16),
              DailyTrendChart(dailyTrend: analytics.dailyTrend),
              const SizedBox(height: AppSpacing.s16),
              InsightCard(insight: analytics.insight),
              const SizedBox(height: AppSpacing.s16),
              _buildAdditionalStats(analytics),
            ],
          ),
        ),
      ),
      loading: () => const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: AppColors.primary),
            SizedBox(height: AppSpacing.s16),
            Text('분석 데이터를 불러오는 중...', style: AppTextStyles.bodyMedium),
          ],
        ),
      ),
      error: (error, _) => Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, color: AppColors.error, size: 64),
            const SizedBox(height: AppSpacing.s16),
            Text(
              '데이터를 불러올 수 없습니다',
              style: AppTextStyles.headlineSmall.copyWith(color: AppColors.textPrimary),
            ),
            const SizedBox(height: AppSpacing.s8),
            Text(
              error.toString(),
              style: AppTextStyles.bodySmall.copyWith(color: AppColors.textSecondary),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.s16),
            ElevatedButton.icon(
              onPressed: () => ref.read(refreshAnalyticsProvider)(),
              icon: const Icon(Icons.refresh),
              label: const Text('다시 시도'),
            ),
          ],
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
        border: Border.all(color: AppColors.divider, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '추가 통계',
            style: AppTextStyles.headlineSmall.copyWith(color: AppColors.textPrimary),
          ),
          const SizedBox(height: AppSpacing.s12),
          _buildStatRow('일평균 알림', '${analytics.averagePerDay.toStringAsFixed(1)}개'),
          const SizedBox(height: AppSpacing.s8),
          if (analytics.mostActiveSource != null) ...[
            _buildStatRow('가장 활발한 소스', analytics.mostActiveSource!.displayName),
            const SizedBox(height: AppSpacing.s8),
          ],
          if (analytics.highestPriority != null)
            _buildStatRow('가장 많은 우선순위', analytics.highestPriority!.displayName),
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
          style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textSecondary),
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

// ---------------------------------------------------------------------------
// Tab 1: AI Token Usage
// ---------------------------------------------------------------------------

class _AiUsageTab extends ConsumerStatefulWidget {
  const _AiUsageTab();

  @override
  ConsumerState<_AiUsageTab> createState() => _AiUsageTabState();
}

class _AiUsageTabState extends ConsumerState<_AiUsageTab> {
  @override
  Widget build(BuildContext context) {
    final filter = ref.watch(aiUsageFilterProvider);
    final aiUsageAsync = ref.watch(aiUsageProvider(filter));

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.s16,
            AppSpacing.s12,
            AppSpacing.s16,
            AppSpacing.s4,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _GranularitySelectorRow(
                selected: filter.granularity,
                onChanged: _onGranularityChanged,
              ),
              const SizedBox(height: AppSpacing.s8),
              _DateRangeSelector(filter: filter, onChanged: _onDateRangeChanged),
            ],
          ),
        ),
        Expanded(
          child: aiUsageAsync.when(
            data: (entity) => RefreshIndicator(
              onRefresh: () async {
                ref.invalidate(aiUsageProvider(filter));
              },
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(AppSpacing.s16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    AiUsageSummaryCard(entity: entity),
                    const SizedBox(height: AppSpacing.s16),
                    TokenTrendChart(trend: entity.trend),
                    const SizedBox(height: AppSpacing.s16),
                    ModelDistributionChart(modelDistribution: entity.modelDistribution),
                    const SizedBox(height: AppSpacing.s16),
                  ],
                ),
              ),
            ),
            loading: () => const Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
            error: (error, _) => Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, color: AppColors.error, size: 48),
                  const SizedBox(height: AppSpacing.s16),
                  Text(
                    error.toString(),
                    style: AppTextStyles.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: AppSpacing.s16),
                  ElevatedButton.icon(
                    onPressed: () => ref.invalidate(aiUsageProvider(filter)),
                    icon: const Icon(Icons.refresh),
                    label: const Text('다시 시도'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  void _onGranularityChanged(AiUsageGranularity granularity) {
    ref.read(aiUsageFilterProvider.notifier).state =
        AiUsageFilter.defaultFor(granularity);
  }

  void _onDateRangeChanged(DateTime start, DateTime end) {
    final current = ref.read(aiUsageFilterProvider);
    ref.read(aiUsageFilterProvider.notifier).state = AiUsageFilter(
      granularity: current.granularity,
      startDate: start,
      endDate: end,
    );
  }
}

// ---------------------------------------------------------------------------
// Granularity selector chips
// ---------------------------------------------------------------------------

class _GranularitySelectorRow extends StatelessWidget {
  final AiUsageGranularity selected;
  final ValueChanged<AiUsageGranularity> onChanged;

  const _GranularitySelectorRow({
    required this.selected,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _chip(AiUsageGranularity.daily, '일별'),
        const SizedBox(width: AppSpacing.s8),
        _chip(AiUsageGranularity.weekly, '주별'),
        const SizedBox(width: AppSpacing.s8),
        _chip(AiUsageGranularity.monthly, '월별'),
      ],
    );
  }

  Widget _chip(AiUsageGranularity granularity, String label) {
    final isSelected = selected == granularity;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (_) => onChanged(granularity),
      selectedColor: AppColors.primary,
      backgroundColor: AppColors.surface,
      labelStyle: AppTextStyles.bodySmall.copyWith(
        color: isSelected ? Colors.white : AppColors.textSecondary,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
      side: BorderSide(
        color: isSelected ? AppColors.primary : AppColors.divider,
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Date range selector
// ---------------------------------------------------------------------------

class _DateRangeSelector extends StatelessWidget {
  final AiUsageFilter filter;
  final void Function(DateTime start, DateTime end) onChanged;

  const _DateRangeSelector({required this.filter, required this.onChanged});

  static final _fmt = DateFormat('yyyy.MM.dd');

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Icon(Icons.calendar_today, size: 16, color: AppColors.textSecondary),
        const SizedBox(width: AppSpacing.s8),
        _dateButton(context, filter.startDate, isStart: true),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.s8),
          child: Text(
            '~',
            style: AppTextStyles.bodySmall.copyWith(color: AppColors.textSecondary),
          ),
        ),
        _dateButton(context, filter.endDate, isStart: false),
      ],
    );
  }

  Widget _dateButton(BuildContext context, DateTime date, {required bool isStart}) {
    return TextButton(
      style: TextButton.styleFrom(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.s8,
          vertical: AppSpacing.s4,
        ),
        minimumSize: Size.zero,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
        foregroundColor: AppColors.primaryLight,
      ),
      onPressed: () => _pickDateRange(context),
      child: Text(
        _fmt.format(date),
        style: AppTextStyles.bodySmall.copyWith(
          color: AppColors.primaryLight,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Future<void> _pickDateRange(BuildContext context) async {
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
      initialDateRange: DateTimeRange(start: filter.startDate, end: filter.endDate),
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.dark(
            primary: AppColors.primary,
            onPrimary: Colors.white,
            surface: AppColors.surface,
            onSurface: AppColors.textPrimary,
          ),
        ),
        child: child!,
      ),
    );

    if (picked != null) {
      onChanged(picked.start, picked.end);
    }
  }
}
