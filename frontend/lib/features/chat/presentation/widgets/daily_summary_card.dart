import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/chat/presentation/providers/chat_providers.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Daily summary card widget (collapsible)
class DailySummaryCard extends ConsumerWidget {
  final bool isExpanded;
  final VoidCallback onToggle;

  const DailySummaryCard({
    required this.isExpanded,
    required this.onToggle,
    super.key,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summaryAsync = ref.watch(dailySummaryProvider);

    return summaryAsync.when(
      data: (summary) => AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
        child: GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              InkWell(
                onTap: onToggle,
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.s16),
                  child: Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          gradient: AppColors.primaryGradient.scale(0.3),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.auto_awesome,
                          color: AppColors.primary,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.s12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Today\'s Summary',
                              style: AppTextStyles.titleMedium.copyWith(
                                color: AppColors.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '${summary.totalMessages} messages',
                              style: AppTextStyles.caption.copyWith(
                                color: AppColors.textTertiary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Icon(
                        isExpanded
                            ? Icons.keyboard_arrow_up
                            : Icons.keyboard_arrow_down,
                        color: AppColors.textSecondary,
                      ),
                    ],
                  ),
                ),
              ),

              // Expandable content
              AnimatedCrossFade(
                duration: const Duration(milliseconds: 300),
                crossFadeState: isExpanded
                    ? CrossFadeState.showFirst
                    : CrossFadeState.showSecond,
                firstChild: Padding(
                  padding: const EdgeInsets.fromLTRB(
                    AppSpacing.s16,
                    0,
                    AppSpacing.s16,
                    AppSpacing.s16,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Divider(color: AppColors.divider),
                      const SizedBox(height: AppSpacing.s12),

                      // Summary text
                      Text(
                        summary.summary,
                        style: AppTextStyles.bodyMedium.copyWith(
                          color: AppColors.textSecondary,
                          height: 1.5,
                        ),
                      ),

                      // Topics
                      if (summary.topics.isNotEmpty) ...[
                        const SizedBox(height: AppSpacing.s16),
                        Text(
                          'Key Topics',
                          style: AppTextStyles.titleSmall.copyWith(
                            color: AppColors.textPrimary,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.s8),
                        Wrap(
                          spacing: AppSpacing.s8,
                          runSpacing: AppSpacing.s8,
                          children: summary.topics.map((topic) {
                            return Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: AppSpacing.s12,
                                vertical: AppSpacing.s4,
                              ),
                              decoration: BoxDecoration(
                                color: AppColors.primaryDark.withValues(alpha: 0.2),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: AppColors.primary.withValues(alpha: 0.3),
                                  width: 1,
                                ),
                              ),
                              child: Text(
                                topic,
                                style: AppTextStyles.caption.copyWith(
                                  color: AppColors.primaryLight,
                                ),
                              ),
                            );
                          }).toList(),
                        ),
                      ],

                      // Cache indicator
                      const SizedBox(height: AppSpacing.s12),
                      Row(
                        children: [
                          const Icon(
                            Icons.schedule,
                            size: 14,
                            color: AppColors.textTertiary,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            'Updated today • Cached for 24h',
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.textTertiary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                secondChild: const SizedBox.shrink(),
              ),
            ],
          ),
        ),
      ),
      loading: () => GlassCard(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Row(
            children: [
              const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                ),
              ),
              const SizedBox(width: AppSpacing.s12),
              Text(
                'Loading today\'s summary...',
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ),
      error: (error, stack) => GlassCard(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Row(
            children: [
              const Icon(
                Icons.error_outline,
                color: AppColors.error,
                size: 20,
              ),
              const SizedBox(width: AppSpacing.s12),
              Expanded(
                child: Text(
                  'Failed to load summary',
                  style: AppTextStyles.bodyMedium.copyWith(
                    color: AppColors.error,
                  ),
                ),
              ),
              TextButton(
                onPressed: () {
                  ref.invalidate(dailySummaryProvider);
                },
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
