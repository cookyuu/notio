import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../../domain/entity/connection_entity.dart';
import '../../domain/entity/connection_auth_type.dart';
import 'connection_status_badge.dart';
import 'connection_provider_icon.dart';

/// Card displaying connection information
class ConnectionCard extends StatelessWidget {
  final ConnectionEntity connection;
  final VoidCallback? onTap;

  const ConnectionCard({
    super.key,
    required this.connection,
    this.onTap,
  });

  String _getAuthTypeLabel() {
    switch (connection.authType) {
      case ConnectionAuthType.apiKey:
        return 'API Key';
      case ConnectionAuthType.oauth:
        return 'OAuth';
      case ConnectionAuthType.signature:
        return 'Signature';
      case ConnectionAuthType.system:
        return 'System';
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: AppSpacing.s12),
        padding: const EdgeInsets.all(AppSpacing.s16),
        decoration: BoxDecoration(
          color: AppColors.glassBackground,
          borderRadius: BorderRadius.circular(AppSpacing.s12),
          border: Border.all(
            color: AppColors.glassBorder,
            width: 0.5,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header: Icon, Name, Status
            Row(
              children: [
                ConnectionProviderIcon.buildIcon(connection.provider),
                const SizedBox(width: AppSpacing.s12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        connection.displayName,
                        style: AppTextStyles.titleMedium,
                        overflow: TextOverflow.ellipsis,
                      ),
                      if (connection.accountLabel != null) ...[
                        const SizedBox(height: AppSpacing.s4),
                        Text(
                          connection.accountLabel!,
                          style: AppTextStyles.bodySmall.copyWith(
                            color: AppColors.textSecondary,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: AppSpacing.s12),
                ConnectionStatusBadge(status: connection.status),
              ],
            ),

            const SizedBox(height: AppSpacing.s12),

            // Auth Type and Key Preview
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.s8,
                    vertical: AppSpacing.s4,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.surface,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    _getAuthTypeLabel(),
                    style: AppTextStyles.labelSmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ),
                if (connection.keyPreview != null) ...[
                  const SizedBox(width: AppSpacing.s8),
                  Text(
                    connection.keyPreview!,
                    style: AppTextStyles.labelSmall.copyWith(
                      color: AppColors.textTertiary,
                      fontFamily: 'monospace',
                    ),
                  ),
                ],
              ],
            ),

            const SizedBox(height: AppSpacing.s12),

            // Timestamps
            Row(
              children: [
                Icon(
                  Icons.access_time,
                  size: 12,
                  color: AppColors.textTertiary,
                ),
                const SizedBox(width: AppSpacing.s4),
                Text(
                  connection.lastUsedAt != null
                      ? 'Used ${timeago.format(connection.lastUsedAt!)}'
                      : 'Never used',
                  style: AppTextStyles.labelSmall.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
                const Spacer(),
                Text(
                  'Created ${timeago.format(connection.createdAt)}',
                  style: AppTextStyles.labelSmall.copyWith(
                    color: AppColors.textTertiary,
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
