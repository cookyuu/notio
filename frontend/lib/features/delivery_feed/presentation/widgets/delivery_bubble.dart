import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';
import 'package:timeago/timeago.dart' as timeago;

class DeliveryBubble extends StatelessWidget {
  const DeliveryBubble({
    required this.item,
    this.onTap,
    super.key,
  });

  final DeliveryFeedItemEntity item;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.s16,
        vertical: AppSpacing.s4,
      ),
      child: GestureDetector(
        onTap: onTap,
        child: GlassCard(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _ChannelAvatar(channelType: item.channelType),
                  const SizedBox(width: AppSpacing.s8),
                  Expanded(
                    child: Text(
                      item.channelDisplayName,
                      style: AppTextStyles.labelMedium.copyWith(
                        color: AppColors.textSecondary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  Text(
                    timeago.format(item.deliveredAt, locale: 'ko'),
                    style: AppTextStyles.labelSmall.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.s8),
              Text(
                item.notificationTitle,
                style: AppTextStyles.bodyMedium.copyWith(
                  fontWeight: FontWeight.w600,
                  color: Colors.white70,
                ),
              ),
              const SizedBox(height: AppSpacing.s4),
              Text(
                item.deliveredContent,
                style: AppTextStyles.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
                maxLines: 5,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ChannelAvatar extends StatelessWidget {
  const _ChannelAvatar({required this.channelType});

  final ChannelTypeEnum channelType;

  @override
  Widget build(BuildContext context) {
    final (icon, color) = switch (channelType) {
      ChannelTypeEnum.slack => (Icons.chat_bubble, const Color(0xFF4A154B)),
      ChannelTypeEnum.telegram => (Icons.send, const Color(0xFF0088CC)),
      ChannelTypeEnum.discord => (Icons.headset, const Color(0xFF5865F2)),
    };

    return Container(
      width: 28,
      height: 28,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Icon(icon, size: 16, color: Colors.white),
    );
  }
}
