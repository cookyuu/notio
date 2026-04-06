import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';
import 'package:notio_app/shared/widgets/source_badge.dart';
import 'package:timeago/timeago.dart' as timeago;

/// Notification card widget
class NotificationCard extends StatelessWidget {
  final NotificationEntity notification;
  final VoidCallback? onTap;
  final VoidCallback? onMarkAsRead;
  final VoidCallback? onDelete;

  const NotificationCard({
    required this.notification,
    super.key,
    this.onTap,
    this.onMarkAsRead,
    this.onDelete,
  });

  Color _getPriorityColor() {
    switch (notification.priority.apiValue) {
      case 'URGENT':
        return AppColors.error;
      case 'HIGH':
        return AppColors.warning;
      case 'MEDIUM':
        return Colors.yellow;
      case 'LOW':
      default:
        return AppColors.textTertiary;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.s16,
        vertical: AppSpacing.s8,
      ),
      child: Slidable(
        key: ValueKey(notification.id),
        endActionPane: ActionPane(
          motion: const ScrollMotion(),
          children: [
            if (!notification.isRead && onMarkAsRead != null)
              SlidableAction(
                onPressed: (_) => onMarkAsRead?.call(),
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                icon: Icons.check,
                label: '읽음',
                borderRadius: BorderRadius.circular(14),
              ),
            if (onDelete != null)
              SlidableAction(
                onPressed: (_) => onDelete?.call(),
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
                icon: Icons.delete,
                label: '삭제',
                borderRadius: BorderRadius.circular(14),
              ),
          ],
        ),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: GlassCard(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
              // Priority indicator
              Container(
                width: 4,
                height: 60,
                decoration: BoxDecoration(
                  color: _getPriorityColor(),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: AppSpacing.s12),

              // Content
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Header row
                    Row(
                      children: [
                        SourceBadge(source: notification.source),
                        const Spacer(),
                        Text(
                          timeago.format(
                            notification.createdAt,
                            locale: 'ko',
                          ),
                          style: AppTextStyles.caption.copyWith(
                            color: AppColors.textTertiary,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.s8),
                        if (!notification.isRead)
                          Container(
                            width: 8,
                            height: 8,
                            decoration: const BoxDecoration(
                              color: AppColors.primary,
                              shape: BoxShape.circle,
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.s8),

                    // Title
                    Text(
                      notification.title,
                      style: AppTextStyles.bodyLarge.copyWith(
                        fontWeight: notification.isRead
                            ? FontWeight.normal
                            : FontWeight.bold,
                        color: notification.isRead
                            ? AppColors.textSecondary
                            : AppColors.textPrimary,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: AppSpacing.s4),

                    // Body
                    Text(
                      notification.body,
                      style: AppTextStyles.bodyMedium.copyWith(
                        color: AppColors.textTertiary,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
            ),
          ),
        ),
      ),
    );
  }
}
