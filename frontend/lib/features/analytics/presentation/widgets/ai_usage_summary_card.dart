import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

class AiUsageSummaryCard extends StatelessWidget {
  final AiUsageEntity entity;

  const AiUsageSummaryCard({required this.entity, super.key});

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'AI 토큰 요약',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _StatItem(
                  label: '총 입력',
                  value: _abbreviate(entity.totalInputTokens),
                  color: AppColors.violet,
                ),
                _StatItem(
                  label: '총 출력',
                  value: _abbreviate(entity.totalOutputTokens),
                  color: AppColors.info,
                ),
                _StatItem(
                  label: '세션',
                  value: _abbreviate(entity.totalSessions),
                  color: AppColors.success,
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.s16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (entity.mostUsedModel != null)
                  Expanded(
                    child: Text(
                      '주요 모델: ${entity.mostUsedModel}',
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                Text(
                  '세션당 ${_abbreviate(entity.avgTokensPerSession.round())} tok',
                  style: AppTextStyles.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
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

class _StatItem extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _StatItem({
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          style: AppTextStyles.headlineLarge.copyWith(
            color: color,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: AppSpacing.s4),
        Text(
          label,
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ],
    );
  }
}
