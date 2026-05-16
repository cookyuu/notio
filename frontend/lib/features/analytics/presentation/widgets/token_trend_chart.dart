import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

class TokenTrendChart extends StatelessWidget {
  final List<AiUsagePeriodPoint> trend;

  const TokenTrendChart({required this.trend, super.key});

  @override
  Widget build(BuildContext context) {
    if (trend.isEmpty) {
      return const GlassCard(
        child: SizedBox(
          height: 200,
          child: Center(
            child: Text('데이터가 없습니다', style: AppTextStyles.bodyMedium),
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
              '토큰 트렌드',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s8),
            const Row(
              children: [
                _LegendDot(color: AppColors.violet, label: '입력'),
                SizedBox(width: AppSpacing.s12),
                _LegendDot(color: AppColors.info, label: '출력'),
              ],
            ),
            const SizedBox(height: AppSpacing.s16),
            SizedBox(
              height: 200,
              child: LineChart(
                LineChartData(
                  gridData: FlGridData(
                    show: true,
                    drawVerticalLine: false,
                    getDrawingHorizontalLine: (_) => const FlLine(
                      color: AppColors.divider,
                      strokeWidth: 1,
                    ),
                  ),
                  titlesData: FlTitlesData(
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
                        interval: _xInterval(),
                        getTitlesWidget: (value, meta) {
                          final index = value.toInt();
                          if (index < 0 || index >= trend.length) {
                            return const SizedBox.shrink();
                          }
                          return Padding(
                            padding: const EdgeInsets.only(top: AppSpacing.s8),
                            child: Text(
                              trend[index].label,
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
                        reservedSize: 42,
                        getTitlesWidget: (value, meta) {
                          return Text(
                            _abbreviate(value.toInt()),
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
                  maxX: (trend.length - 1).toDouble(),
                  minY: 0,
                  maxY: _maxY(),
                  lineBarsData: [
                    _buildBar(
                      spots: _spots((p) => p.inputTokens),
                      color: AppColors.violet,
                    ),
                    _buildBar(
                      spots: _spots((p) => p.outputTokens),
                      color: AppColors.info,
                    ),
                  ],
                  lineTouchData: LineTouchData(
                    touchTooltipData: LineTouchTooltipData(
                      getTooltipItems: (touchedSpots) {
                        return touchedSpots.map((spot) {
                          final index = spot.x.toInt();
                          final label =
                              index < trend.length ? trend[index].label : '';
                          final isInput = spot.barIndex == 0;
                          return LineTooltipItem(
                            '$label\n${isInput ? "입력" : "출력"}: ${_abbreviate(spot.y.toInt())}',
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

  List<FlSpot> _spots(int Function(AiUsagePeriodPoint) selector) {
    return List.generate(
      trend.length,
      (i) => FlSpot(i.toDouble(), selector(trend[i]).toDouble()),
    );
  }

  LineChartBarData _buildBar({
    required List<FlSpot> spots,
    required Color color,
  }) {
    return LineChartBarData(
      spots: spots,
      isCurved: true,
      color: color,
      barWidth: 2,
      isStrokeCapRound: true,
      dotData: const FlDotData(show: false),
      belowBarData: BarAreaData(
        show: true,
        color: color.withValues(alpha: 0.15),
      ),
    );
  }

  double _maxY() {
    var max = 0;
    for (final p in trend) {
      if (p.inputTokens > max) max = p.inputTokens;
      if (p.outputTokens > max) max = p.outputTokens;
    }
    return (max * 1.2).ceilToDouble();
  }

  double _xInterval() {
    if (trend.length <= 7) return 1;
    return (trend.length / 6).ceilToDouble();
  }

  String _abbreviate(int value) {
    if (value >= 1000000) {
      final m = value / 1000000;
      return '${m % 1 == 0 ? m.toInt() : m.toStringAsFixed(1)}M';
    }
    if (value >= 1000) {
      final k = value / 1000;
      return '${k % 1 == 0 ? k.toInt() : k.toStringAsFixed(1)}K';
    }
    return value.toString();
  }
}

class _LegendDot extends StatelessWidget {
  final Color color;
  final String label;

  const _LegendDot({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: AppSpacing.s4),
        Text(
          label,
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
            fontSize: 11,
          ),
        ),
      ],
    );
  }
}
