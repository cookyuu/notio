import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';
import 'package:notio_app/shared/widgets/source_badge.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'package:url_launcher/url_launcher.dart';

/// Modal for displaying notification details
class NotificationDetailModal extends StatelessWidget {
  final NotificationDetailEntity notification;

  const NotificationDetailModal({
    required this.notification,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.7,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      builder: (context, scrollController) {
        return Container(
          decoration: const BoxDecoration(
            color: AppColors.background,
            borderRadius: BorderRadius.vertical(
              top: Radius.circular(20),
            ),
          ),
          child: Column(
            children: [
              // Drag handle
              Container(
                margin: const EdgeInsets.symmetric(vertical: AppSpacing.s12),
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.textSecondary.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),

              // Content
              Expanded(
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.all(AppSpacing.s20),
                  children: [
                    // Header with source badge and timestamp
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        SourceBadge(source: notification.source),
                        Text(
                          timeago.format(
                            notification.createdAt,
                            locale: 'ko',
                          ),
                          style: AppTextStyles.bodySmall.copyWith(
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: AppSpacing.s20),

                    // Title
                    Text(
                      notification.title,
                      style: AppTextStyles.headlineMedium.copyWith(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.bold,
                      ),
                    ),

                    const SizedBox(height: AppSpacing.s16),

                    // Body
                    GlassCard(
                      child: Padding(
                        padding: const EdgeInsets.all(AppSpacing.s16),
                        child: SelectableText(
                          notification.body,
                          style: AppTextStyles.bodyLarge.copyWith(
                            color: AppColors.textPrimary,
                            height: 1.6,
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: AppSpacing.s20),

                    // Priority indicator
                    _buildPrioritySection(),

                    if (notification.externalUrl != null) ...[
                      const SizedBox(height: AppSpacing.s20),
                      _buildExternalLinkSection(
                        context,
                        notification.externalUrl!,
                      ),
                    ],

                    if (notification.metadata != null &&
                        notification.metadata!.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.s20),
                      _buildMetadataSection(notification.metadata!),
                    ],

                    const SizedBox(height: AppSpacing.s20),

                    // Action buttons
                    _buildActionButtons(context),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildPrioritySection() {
    final priorityColor = switch (notification.priority) {
      NotificationPriority.urgent => AppColors.error,
      NotificationPriority.high => AppColors.warning,
      NotificationPriority.medium => AppColors.violet2,
      NotificationPriority.low => AppColors.textSecondary,
    };

    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s16),
        child: Row(
          children: [
            Icon(
              Icons.flag,
              color: priorityColor,
              size: 20,
            ),
            const SizedBox(width: AppSpacing.s12),
            Text(
              '우선순위: ${notification.priority.displayName}',
              style: AppTextStyles.bodyMedium.copyWith(
                color: priorityColor,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildExternalLinkSection(BuildContext context, String url) {
    return GlassCard(
      child: InkWell(
        onTap: () => _launchUrl(url),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Row(
            children: [
              const Icon(
                Icons.link,
                color: AppColors.primary,
                size: 20,
              ),
              const SizedBox(width: AppSpacing.s12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '외부 링크',
                      style: AppTextStyles.bodyMedium,
                    ),
                    const SizedBox(height: AppSpacing.s4),
                    Text(
                      url,
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.primary,
                        decoration: TextDecoration.underline,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.open_in_new,
                color: AppColors.textSecondary,
                size: 16,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMetadataSection(Map<String, dynamic> metadata) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '추가 정보',
          style: AppTextStyles.headlineSmall.copyWith(
            color: AppColors.text1,
          ),
        ),
        const SizedBox(height: AppSpacing.s12),
        GlassCard(
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.s16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: metadata.entries.map((entry) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.s8),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 100,
                        child: Text(
                          '${entry.key}:',
                          style: AppTextStyles.bodyMedium.copyWith(
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ),
                      Expanded(
                        child: Text(
                          entry.value.toString(),
                          style: AppTextStyles.bodyMedium,
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildActionButtons(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: OutlinedButton.icon(
            onPressed: () {
              // Copy notification content to clipboard
              Clipboard.setData(
                ClipboardData(
                  text: '${notification.title}\n\n${notification.body}',
                ),
              );

              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  backgroundColor: AppColors.bg3,
                  content: Text(
                    '클립보드에 복사했습니다',
                    style: AppTextStyles.bodySmall.copyWith(
                      color: AppColors.text1,
                    ),
                  ),
                  duration: const Duration(seconds: 2),
                ),
              );
            },
            icon: const Icon(Icons.copy),
            label: const Text('Copy'),
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.textPrimary,
              side: const BorderSide(color: AppColors.divider),
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.s12),
        Expanded(
          child: FilledButton.icon(
            onPressed: () {
              Navigator.of(context).pop();
            },
            icon: const Icon(Icons.close),
            label: const Text('Close'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _launchUrl(String urlString) async {
    final Uri url = Uri.parse(urlString);
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      debugPrint('Could not launch $urlString');
    }
  }
}
