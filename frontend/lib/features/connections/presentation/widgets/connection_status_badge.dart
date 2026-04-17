import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../../domain/entity/connection_status.dart';

/// Badge displaying connection status with appropriate color
class ConnectionStatusBadge extends StatelessWidget {
  final ConnectionStatus status;

  const ConnectionStatusBadge({
    required this.status,
    super.key,
  });

  Color _getColor() {
    switch (status) {
      case ConnectionStatus.active:
        return AppColors.success;
      case ConnectionStatus.pending:
        return AppColors.warning;
      case ConnectionStatus.needsAction:
        return AppColors.error;
      case ConnectionStatus.revoked:
        return AppColors.textTertiary;
      case ConnectionStatus.error:
        return AppColors.error;
    }
  }

  String _getLabel() {
    switch (status) {
      case ConnectionStatus.active:
        return 'Active';
      case ConnectionStatus.pending:
        return 'Pending';
      case ConnectionStatus.needsAction:
        return 'Needs Action';
      case ConnectionStatus.revoked:
        return 'Revoked';
      case ConnectionStatus.error:
        return 'Error';
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _getColor();

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.s8,
        vertical: AppSpacing.s4,
      ),
      decoration: BoxDecoration(
        color: color.withAlpha(38), // 15% opacity
        borderRadius: BorderRadius.circular(6),
        border: Border.all(
          color: color.withAlpha(77), // 30% opacity
          width: 0.5,
        ),
      ),
      child: Text(
        _getLabel(),
        style: AppTextStyles.labelSmall.copyWith(
          color: color,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
