import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';

/// Badge widget for notification sources
class SourceBadge extends StatelessWidget {
  const SourceBadge({
    required this.source,
    this.size = SourceBadgeSize.medium,
    super.key,
  });

  final NotificationSource source;
  final SourceBadgeSize size;

  Color get _backgroundColor {
    switch (source) {
      case NotificationSource.claude:
        return AppColors.claudeBadge;
      case NotificationSource.codex:
        return AppColors.codexBadge;
      case NotificationSource.slack:
        return AppColors.slackBadge;
      case NotificationSource.github:
        return AppColors.githubBadge;
      case NotificationSource.gmail:
        return AppColors.gmailBadge;
    }
  }

  IconData get _icon {
    switch (source) {
      case NotificationSource.claude:
        return Icons.auto_awesome;
      case NotificationSource.codex:
        return Icons.terminal;
      case NotificationSource.slack:
        return Icons.tag;
      case NotificationSource.github:
        return Icons.code;
      case NotificationSource.gmail:
        return Icons.email;
    }
  }

  double get _iconSize {
    switch (size) {
      case SourceBadgeSize.small:
        return 12;
      case SourceBadgeSize.medium:
        return 14;
      case SourceBadgeSize.large:
        return 16;
    }
  }

  TextStyle get _textStyle {
    final baseStyle = switch (size) {
      SourceBadgeSize.small => AppTextStyles.labelSmall,
      SourceBadgeSize.medium => AppTextStyles.labelMedium,
      SourceBadgeSize.large => AppTextStyles.labelLarge,
    };
    return baseStyle.copyWith(color: AppColors.textPrimary);
  }

  EdgeInsets get _padding {
    switch (size) {
      case SourceBadgeSize.small:
        return const EdgeInsets.symmetric(
          horizontal: AppSpacing.s8,
          vertical: AppSpacing.s4,
        );
      case SourceBadgeSize.medium:
        return const EdgeInsets.symmetric(
          horizontal: AppSpacing.s12,
          vertical: AppSpacing.s4,
        );
      case SourceBadgeSize.large:
        return const EdgeInsets.symmetric(
          horizontal: AppSpacing.s16,
          vertical: AppSpacing.s8,
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: _padding,
      decoration: BoxDecoration(
        color: _backgroundColor,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            _icon,
            size: _iconSize,
            color: AppColors.textPrimary,
          ),
          const SizedBox(width: AppSpacing.s4),
          Text(
            source.displayName,
            style: _textStyle,
          ),
        ],
      ),
    );
  }
}

enum SourceBadgeSize {
  small,
  medium,
  large,
}
