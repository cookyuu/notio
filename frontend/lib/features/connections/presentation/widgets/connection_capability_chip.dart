import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../../domain/entity/connection_capability.dart';

/// Chip displaying connection capability
class ConnectionCapabilityChip extends StatelessWidget {
  final ConnectionCapability capability;

  const ConnectionCapabilityChip({
    required this.capability,
    super.key,
  });

  IconData _getIcon() {
    switch (capability) {
      case ConnectionCapability.webhookReceive:
        return Icons.webhook;
      case ConnectionCapability.testMessage:
        return Icons.send_outlined;
      case ConnectionCapability.refreshToken:
        return Icons.refresh;
      case ConnectionCapability.rotateKey:
        return Icons.vpn_key_outlined;
    }
  }

  String _getLabel() {
    switch (capability) {
      case ConnectionCapability.webhookReceive:
        return 'Webhook';
      case ConnectionCapability.testMessage:
        return 'Test';
      case ConnectionCapability.refreshToken:
        return 'Refresh';
      case ConnectionCapability.rotateKey:
        return 'Rotate';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.s8,
        vertical: AppSpacing.s4,
      ),
      decoration: BoxDecoration(
        color: AppColors.primary.withAlpha(38), // 15% opacity
        borderRadius: BorderRadius.circular(6),
        border: Border.all(
          color: AppColors.primary.withAlpha(77), // 30% opacity
          width: 0.5,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            _getIcon(),
            size: 12,
            color: AppColors.primary,
          ),
          const SizedBox(width: AppSpacing.s4),
          Text(
            _getLabel(),
            style: AppTextStyles.labelSmall.copyWith(
              color: AppColors.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
