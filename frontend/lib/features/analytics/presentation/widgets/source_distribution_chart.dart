import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Widget displaying notification source distribution as a donut chart
class SourceDistributionChart extends StatefulWidget {
  final Map<NotificationSource, int> distribution;

  const SourceDistributionChart({
    required this.distribution,
    super.key,
  });

  @override
  State<SourceDistributionChart> createState() =>
      _SourceDistributionChartState();
}

class _SourceDistributionChartState extends State<SourceDistributionChart> {
  int? touchedIndex;

  @override
  Widget build(BuildContext context) {
    // Calculate total
    final total = widget.distribution.values.fold<int>(
      0,
      (sum, value) => sum + value,
    );

    if (total == 0) {
      return const GlassCard(
        child: SizedBox(
          height: 300,
          child: Center(
            child: Text(
              '데이터가 없습니다',
              style: AppTextStyles.bodyMedium,
            ),
          ),
        ),
      );
    }

    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '소스별 분포',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s16),
            SizedBox(
              height: 200,
              child: Row(
                children: [
                  // Chart
                  Expanded(
                    flex: 2,
                    child: PieChart(
                      PieChartData(
                        pieTouchData: PieTouchData(
                          touchCallback: (FlTouchEvent event, pieTouchResponse) {
                            setState(() {
                              if (!event.isInterestedForInteractions ||
                                  pieTouchResponse == null ||
                                  pieTouchResponse.touchedSection == null) {
                                touchedIndex = null;
                                return;
                              }
                              touchedIndex = pieTouchResponse
                                  .touchedSection!.touchedSectionIndex;
                            });
                          },
                        ),
                        borderData: FlBorderData(show: false),
                        sectionsSpace: 2,
                        centerSpaceRadius: 50,
                        sections: _buildSections(total),
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.s16),
                  // Legend
                  Expanded(
                    flex: 1,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: _buildLegend(total),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<PieChartSectionData> _buildSections(int total) {
    final entries = widget.distribution.entries.toList();

    return List.generate(entries.length, (index) {
      final entry = entries[index];
      final isTouched = index == touchedIndex;
      final radius = isTouched ? 65.0 : 55.0;
      final fontSize = isTouched ? 16.0 : 12.0;

      final percentage = (entry.value / total * 100).toStringAsFixed(1);

      return PieChartSectionData(
        color: _getSourceColor(entry.key),
        value: entry.value.toDouble(),
        title: '$percentage%',
        radius: radius,
        titleStyle: AppTextStyles.bodySmall.copyWith(
          fontSize: fontSize,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
      );
    });
  }

  List<Widget> _buildLegend(int total) {
    return widget.distribution.entries.map((entry) {
      final percentage = (entry.value / total * 100).toStringAsFixed(1);

      return Padding(
        padding: const EdgeInsets.only(bottom: AppSpacing.s8),
        child: Row(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: _getSourceColor(entry.key),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(width: AppSpacing.s8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    entry.key.displayName,
                    style: AppTextStyles.bodySmall.copyWith(
                      color: AppColors.textPrimary,
                      fontSize: 10,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    '${entry.value} ($percentage%)',
                    style: AppTextStyles.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                      fontSize: 9,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }).toList();
  }

  Color _getSourceColor(NotificationSource source) {
    switch (source) {
      case NotificationSource.claude:
        return AppColors.claudeBadge;
      case NotificationSource.codex:
        return AppColors.codexBadge;
      case NotificationSource.slack:
        return AppColors.slackBadge;
      case NotificationSource.github:
        return AppColors.githubBadge;
      case NotificationSource.gmail:
        return AppColors.gmailBadge;
    }
  }
}
