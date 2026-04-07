import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Widget displaying AI-generated insight
class InsightCard extends StatelessWidget {
  final String insight;

  const InsightCard({
    super.key,
    required this.insight,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Icon
            Container(
              padding: const EdgeInsets.all(AppSpacing.s8),
              decoration: BoxDecoration(
                color: AppColors.primary.withOpacity(0.2),
                borderRadius: BorderRadius.circular(AppSpacing.s8),
              ),
              child: const Icon(
                Icons.lightbulb_outline,
                color: AppColors.primary,
                size: 24,
              ),
            ),
            const SizedBox(width: AppSpacing.s12),
            // Content
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'AI 인사이트',
                    style: AppTextStyles.headlineSmall.copyWith(
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.s8),
                  Text(
                    insight,
                    style: AppTextStyles.bodyMedium.copyWith(
                      color: AppColors.textSecondary,
                      height: 1.5,
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
}
