import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import '../../domain/entity/connection_provider.dart';

/// Helper to get provider icon
class ConnectionProviderIcon {
  ConnectionProviderIcon._();

  static IconData getIcon(ConnectionProvider provider) {
    switch (provider) {
      case ConnectionProvider.claude:
        return Icons.smart_toy_outlined;
      case ConnectionProvider.slack:
        return Icons.chat_bubble_outline;
      case ConnectionProvider.gmail:
        return Icons.email_outlined;
      case ConnectionProvider.github:
        return Icons.code_outlined;
      case ConnectionProvider.discord:
        return Icons.forum_outlined;
      case ConnectionProvider.jira:
        return Icons.task_outlined;
      case ConnectionProvider.linear:
        return Icons.linear_scale_outlined;
      case ConnectionProvider.teams:
        return Icons.groups_outlined;
    }
  }

  static Color getColor(ConnectionProvider provider) {
    switch (provider) {
      case ConnectionProvider.claude:
        return AppColors.primary;
      case ConnectionProvider.slack:
        return const Color(0xFFE01E5A);
      case ConnectionProvider.gmail:
        return const Color(0xFFEA4335);
      case ConnectionProvider.github:
        return const Color(0xFF6CC644);
      case ConnectionProvider.discord:
        return const Color(0xFF5865F2);
      case ConnectionProvider.jira:
        return const Color(0xFF0052CC);
      case ConnectionProvider.linear:
        return const Color(0xFF5E6AD2);
      case ConnectionProvider.teams:
        return const Color(0xFF6264A7);
    }
  }

  static Widget buildIcon(ConnectionProvider provider, {double size = 24}) {
    return Icon(
      getIcon(provider),
      color: getColor(provider),
      size: size,
    );
  }
}
