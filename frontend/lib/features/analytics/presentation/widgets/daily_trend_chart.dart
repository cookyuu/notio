import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Widget displaying daily notification trend as a line chart
class DailyTrendChart extends StatelessWidget {
  final Map<DateTime, int> dailyTrend;

  const DailyTrendChart({
    required this.dailyTrend,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    if (dailyTrend.isEmpty) {
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
              '일별 트렌드 (7일)',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s16),
            SizedBox(
              height: 200,
              child: LineChart(
                LineChartData(
                  gridData: FlGridData(
                    show: true,
                    drawVerticalLine: false,
                    getDrawingHorizontalLine: (value) {
                      return const FlLine(
                        color: AppColors.divider,
                        strokeWidth: 1,
                      );
                    },
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
                        reservedSize: 30,
                        interval: 1,
                        getTitlesWidget: (value, meta) {
                          final sortedDates = dailyTrend.keys.toList()
                            ..sort();
                          final index = value.toInt();

                          if (index < 0 || index >= sortedDates.length) {
                            return const SizedBox.shrink();
                          }

                          final date = sortedDates[index];
                          final weekday = _getWeekdayName(date.weekday);

                          return Padding(
                            padding: const EdgeInsets.only(top: AppSpacing.s8),
                            child: Text(
                              weekday,
                              style: AppTextStyles.bodySmall.copyWith(
                                color: AppColors.textSecondary,
                                fontSize: 10,
                              ),
                            ),
                          );
                        },
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
                  minX: 0,
                  maxX: (dailyTrend.length - 1).toDouble(),
                  minY: 0,
                  maxY: _getMaxY(),
                  lineBarsData: [
                    LineChartBarData(
                      spots: _buildSpots(),
                      isCurved: true,
                      color: AppColors.primary,
                      barWidth: 3,
                      isStrokeCapRound: true,
                      dotData: FlDotData(
                        show: true,
                        getDotPainter: (spot, percent, barData, index) {
                          return FlDotCirclePainter(
                            radius: 4,
                            color: AppColors.primary,
                            strokeWidth: 2,
                            strokeColor: AppColors.background,
                          );
                        },
                      ),
                      belowBarData: BarAreaData(
                        show: true,
                        color: AppColors.primary.withValues(alpha: 0.2),
                      ),
                    ),
                  ],
                  lineTouchData: LineTouchData(
                    touchTooltipData: LineTouchTooltipData(
                      getTooltipItems: (touchedSpots) {
                        return touchedSpots.map((spot) {
                          final sortedDates = dailyTrend.keys.toList()..sort();
                          final date = sortedDates[spot.x.toInt()];
                          final formattedDate =
                              '${date.month}/${date.day}';

                          return LineTooltipItem(
                            '$formattedDate\n${spot.y.toInt()}개',
                            AppTextStyles.bodySmall.copyWith(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                            ),
                          );
                        }).toList();
                      },
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<FlSpot> _buildSpots() {
    final sortedEntries = dailyTrend.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));

    return List.generate(
      sortedEntries.length,
      (index) => FlSpot(
        index.toDouble(),
        sortedEntries[index].value.toDouble(),
      ),
    );
  }

  double _getMaxY() {
    final maxValue = dailyTrend.values.fold<int>(
      0,
      (max, value) => value > max ? value : max,
    );

    // Add 20% padding
    return (maxValue * 1.2).ceilToDouble();
  }

  String _getWeekdayName(int weekday) {
    switch (weekday) {
      case DateTime.monday:
        return '월';
      case DateTime.tuesday:
        return '화';
      case DateTime.wednesday:
        return '수';
      case DateTime.thursday:
        return '목';
      case DateTime.friday:
        return '금';
      case DateTime.saturday:
        return '토';
      case DateTime.sunday:
        return '일';
      default:
        return '';
    }
  }
}
