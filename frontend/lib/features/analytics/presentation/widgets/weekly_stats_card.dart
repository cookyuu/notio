import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Widget displaying weekly notification statistics
class WeeklyStatsCard extends StatelessWidget {
  final int totalNotifications;
  final int unreadNotifications;
  final double readPercentage;

  const WeeklyStatsCard({
    required this.totalNotifications,
    required this.unreadNotifications,
    required this.readPercentage,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final readNotifications = totalNotifications - unreadNotifications;

    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '주간 알림 요약',
              style: AppTextStyles.headlineSmall.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _StatItem(
                  label: '전체',
                  value: totalNotifications.toString(),
                  color: AppColors.primary,
                ),
                _StatItem(
                  label: '읽음',
                  value: readNotifications.toString(),
                  color: AppColors.success,
                ),
                _StatItem(
                  label: '미읽음',
                  value: unreadNotifications.toString(),
                  color: AppColors.warning,
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.s16),
            // Read percentage bar
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      '읽음 비율',
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    Text(
                      '${readPercentage.toStringAsFixed(1)}%',
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.success,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.s8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(AppSpacing.s4),
                  child: LinearProgressIndicator(
                    value: readPercentage / 100,
                    backgroundColor: AppColors.surfaceLight,
                    valueColor: const AlwaysStoppedAnimation<Color>(
                      AppColors.success,
                    ),
                    minHeight: 8,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
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
