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
            color: AppColors.bg3,
            borderRadius: BorderRadius.vertical(
              top: Radius.circular(20),
            ),
          ),
          child: SafeArea(
            top: false,
            child: Column(
              children: [
                Container(
                  margin: const EdgeInsets.symmetric(vertical: AppSpacing.s12),
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.text2.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                Expanded(
                  child: ListView(
                    controller: scrollController,
                    padding: const EdgeInsets.fromLTRB(
                      AppSpacing.s20,
                      AppSpacing.s8,
                      AppSpacing.s20,
                      AppSpacing.s24,
                    ),
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          SourceBadge(source: notification.source),
                          const SizedBox(width: AppSpacing.s12),
                          Expanded(
                            child: Text(
                              timeago.format(
                                notification.createdAt,
                                locale: 'ko',
                              ),
                              textAlign: TextAlign.end,
                              style: AppTextStyles.caption.copyWith(
                                color: AppColors.text2,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: AppSpacing.s20),
                      Text(
                        notification.title,
                        style: AppTextStyles.displaySmall.copyWith(
                          color: AppColors.text1,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.s16),
                      GlassCard(
                        child: Padding(
                          padding: const EdgeInsets.all(AppSpacing.s16),
                          child: SelectableText(
                            notification.body,
                            style: AppTextStyles.bodyLarge.copyWith(
                              color: AppColors.text1,
                              height: 1.65,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: AppSpacing.s20),
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
                      _buildActionButtons(context),
                    ],
                  ),
                ),
              ],
            ),
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
              style: AppTextStyles.titleSmall.copyWith(
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
                      style: AppTextStyles.titleSmall,
                    ),
                    const SizedBox(height: AppSpacing.s4),
                    Text(
                      url,
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.violet2,
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
                            color: AppColors.text2,
                          ),
                        ),
                      ),
                      Expanded(
                        child: SelectableText(
                          _formatMetadataValue(entry.value),
                          style: AppTextStyles.bodyMedium.copyWith(
                            color: AppColors.text1,
                          ),
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        OutlinedButton.icon(
          onPressed: () => _copyDetailToClipboard(context),
          icon: const Icon(Icons.copy_rounded),
          label: const Text('복사'),
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.text1,
            side: const BorderSide(color: AppColors.border2),
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.s16),
          ),
        ),
        if (notification.externalUrl != null) ...[
          const SizedBox(height: AppSpacing.s12),
          FilledButton.icon(
            onPressed: () => _launchUrl(notification.externalUrl!),
            icon: const Icon(Icons.open_in_new_rounded),
            label: const Text('외부 링크 열기'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.violet,
              foregroundColor: AppColors.text1,
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.s16),
            ),
          ),
        ],
        const SizedBox(height: AppSpacing.s12),
        TextButton.icon(
          onPressed: () {
            Navigator.of(context).pop();
          },
          icon: const Icon(Icons.close_rounded),
          label: const Text('닫기'),
          style: TextButton.styleFrom(
            foregroundColor: AppColors.text2,
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.s12),
          ),
        ),
      ],
    );
  }

  Future<void> _copyDetailToClipboard(BuildContext context) async {
    await Clipboard.setData(
      ClipboardData(text: _buildCopyPayload()),
    );

    if (!context.mounted) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        backgroundColor: AppColors.bg2,
        content: Text(
          '상세 내용을 클립보드에 복사했습니다',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.text1,
          ),
        ),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  String _buildCopyPayload() {
    final buffer = StringBuffer()
      ..writeln(notification.title)
      ..writeln()
      ..writeln(notification.body);

    if (notification.externalUrl != null) {
      buffer
        ..writeln()
        ..writeln('외부 링크')
        ..writeln(notification.externalUrl);
    }

    if (notification.metadata != null && notification.metadata!.isNotEmpty) {
      buffer.writeln();
      buffer.writeln('추가 정보');
      for (final entry in notification.metadata!.entries) {
        buffer.writeln('${entry.key}: ${_formatMetadataValue(entry.value)}');
      }
    }

    return buffer.toString().trimRight();
  }

  String _formatMetadataValue(Object? value) {
    if (value == null) {
      return '-';
    }

    if (value is Iterable || value is Map<String, dynamic>) {
      return value.toString();
    }

    return '$value';
  }

  Future<void> _launchUrl(String urlString) async {
    final Uri url = Uri.parse(urlString);
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      debugPrint('Could not launch $urlString');
    }
  }
}
