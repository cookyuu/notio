import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

class ModelDistributionChart extends StatefulWidget {
  final List<AiUsageModelShare> modelDistribution;

  const ModelDistributionChart({required this.modelDistribution, super.key});

  @override
  State<ModelDistributionChart> createState() => _ModelDistributionChartState();
}

class _ModelDistributionChartState extends State<ModelDistributionChart> {
  static const _colors = [
    AppColors.primary,
    AppColors.info,
    AppColors.success,
    AppColors.warning,
    AppColors.error,
  ];

  int? touchedIndex;

  @override
  Widget build(BuildContext context) {
    final distribution = widget.modelDistribution;

    if (distribution.isEmpty) {
      return const GlassCard(
        child: SizedBox(
          height: 300,
          child: Center(
            child: Text('데이터가 없습니다', style: AppTextStyles.bodyMedium),
          ),
        ),
      );
    }

    final total = distribution.fold<int>(0, (sum, m) => sum + m.totalTokens);
    final isSingle = distribution.length == 1;

    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '모델별 분포',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s16),
            SizedBox(
              height: 200,
              child: Row(
                children: [
                  Expanded(
                    flex: 2,
                    child: PieChart(
                      PieChartData(
                        pieTouchData: PieTouchData(
                          touchCallback: (FlTouchEvent event, response) {
                            setState(() {
                              if (!event.isInterestedForInteractions ||
                                  response == null ||
                                  response.touchedSection == null) {
                                touchedIndex = null;
                                return;
                              }
                              touchedIndex = response
                                  .touchedSection!.touchedSectionIndex;
                            });
                          },
                        ),
                        borderData: FlBorderData(show: false),
                        sectionsSpace: isSingle ? 0 : 2,
                        centerSpaceRadius: isSingle ? 0 : 50,
                        sections: _buildSections(total),
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.s16),
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
    return List.generate(widget.modelDistribution.length, (index) {
      final item = widget.modelDistribution[index];
      final isTouched = index == touchedIndex;
      final radius = isTouched ? 65.0 : 55.0;
      final percentage = (item.totalTokens / total * 100).toStringAsFixed(1);

      return PieChartSectionData(
        color: _colors[index % _colors.length],
        value: item.totalTokens.toDouble(),
        title: '$percentage%',
        radius: radius,
        titleStyle: AppTextStyles.bodySmall.copyWith(
          fontSize: isTouched ? 14.0 : 11.0,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
      );
    });
  }

  List<Widget> _buildLegend(int total) {
    return List.generate(widget.modelDistribution.length, (index) {
      final item = widget.modelDistribution[index];
      final color = _colors[index % _colors.length];
      final truncatedModel = item.model.length > 20
          ? '${item.model.substring(0, 20)}…'
          : item.model;
      final formattedTokens = _formatTokens(item.totalTokens);

      return Padding(
        padding: const EdgeInsets.only(bottom: AppSpacing.s8),
        child: Row(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: color,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(width: AppSpacing.s8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    truncatedModel,
                    style: AppTextStyles.bodySmall.copyWith(
                      color: AppColors.textPrimary,
                      fontSize: 10,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    '$formattedTokens tok',
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
    });
  }

  String _formatTokens(int value) {
    if (value >= 1000) {
      final formatted = value.toString();
      final buffer = StringBuffer();
      var count = 0;
      for (var i = formatted.length - 1; i >= 0; i--) {
        if (count > 0 && count % 3 == 0) buffer.write(',');
        buffer.write(formatted[i]);
        count++;
      }
      return buffer.toString().split('').reversed.join();
    }
    return value.toString();
  }
}
