import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Widget displaying notification priority distribution as a bar chart
class PriorityDistributionChart extends StatelessWidget {
  final Map<NotificationPriority, int> distribution;

  const PriorityDistributionChart({
    super.key,
    required this.distribution,
  });

  @override
  Widget build(BuildContext context) {
    // Check if empty
    if (distribution.isEmpty ||
        distribution.values.every((value) => value == 0)) {
      return const GlassCard(
        child: SizedBox(
          height: 250,
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
              '우선순위별 분포',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s16),
            SizedBox(
              height: 200,
              child: BarChart(
                BarChartData(
                  alignment: BarChartAlignment.spaceAround,
                  maxY: _getMaxY(),
                  barTouchData: BarTouchData(
                    touchTooltipData: BarTouchTooltipData(
                      getTooltipItem: (group, groupIndex, rod, rodIndex) {
                        final priority = _getPriorityByIndex(groupIndex);
                        return BarTooltipItem(
                          '${priority.displayName}\n${rod.toY.toInt()}',
                          AppTextStyles.bodySmall.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        );
                      },
                    ),
                  ),
                  titlesData: FlTitlesData(
                    show: true,
                    rightTitles: const AxisTitles(
                      sideTitles: SideTitles(showTitles: false),
                    ),
                    topTitles: const AxisTitles(
                      sideTitles: SideTitles(showTitles: false),
                    ),
                    bottomTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        getTitlesWidget: (value, meta) {
                          final priority = _getPriorityByIndex(value.toInt());
                          return Padding(
                            padding: const EdgeInsets.only(top: AppSpacing.s8),
                            child: Text(
                              priority.displayName,
                              style: AppTextStyles.bodySmall.copyWith(
                                color: AppColors.textSecondary,
                                fontSize: 10,
                              ),
                            ),
                          );
                        },
                        reservedSize: 30,
                      ),
                    ),
                    leftTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        reservedSize: 30,
                        getTitlesWidget: (value, meta) {
                          return Text(
                            value.toInt().toString(),
                            style: AppTextStyles.bodySmall.copyWith(
                              color: AppColors.textSecondary,
                              fontSize: 10,
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                  borderData: FlBorderData(show: false),
                  gridData: FlGridData(
                    show: true,
                    drawVerticalLine: false,
                    getDrawingHorizontalLine: (value) {
                      return FlLine(
                        color: AppColors.divider,
                        strokeWidth: 1,
                      );
                    },
                  ),
                  barGroups: _buildBarGroups(),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<BarChartGroupData> _buildBarGroups() {
    return [
      _buildBarGroup(0, NotificationPriority.urgent),
      _buildBarGroup(1, NotificationPriority.high),
      _buildBarGroup(2, NotificationPriority.medium),
      _buildBarGroup(3, NotificationPriority.low),
    ];
  }

  BarChartGroupData _buildBarGroup(int x, NotificationPriority priority) {
    final value = distribution[priority]?.toDouble() ?? 0.0;

    return BarChartGroupData(
      x: x,
      barRods: [
        BarChartRodData(
          toY: value,
          color: _getPriorityColor(priority),
          width: 24,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(4),
            topRight: Radius.circular(4),
          ),
        ),
      ],
    );
  }

  double _getMaxY() {
    final maxValue = distribution.values.fold<int>(
      0,
      (max, value) => value > max ? value : max,
    );

    // Add 20% padding
    return (maxValue * 1.2).ceilToDouble();
  }

  NotificationPriority _getPriorityByIndex(int index) {
    switch (index) {
      case 0:
        return NotificationPriority.urgent;
      case 1:
        return NotificationPriority.high;
      case 2:
        return NotificationPriority.medium;
      case 3:
        return NotificationPriority.low;
      default:
        return NotificationPriority.low;
    }
  }

  Color _getPriorityColor(NotificationPriority priority) {
    switch (priority) {
      case NotificationPriority.urgent:
        return AppColors.error;
      case NotificationPriority.high:
        return AppColors.warning;
      case NotificationPriority.medium:
        return AppColors.info;
      case NotificationPriority.low:
        return AppColors.success;
    }
  }
}
