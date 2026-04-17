import 'package:flutter/material.dart';

/// App color palette with dark mode and violet accent
class AppColors {
  AppColors._();

  // Background colors
  static const Color bg0 = Color(0xFF0A0A12);
  static const Color bg1 = Color(0xFF0F0F1A);
  static const Color bg2 = Color(0xFF141428);
  static const Color bg3 = Color(0xFF1C1C35);

  // Accent colors
  static const Color violet = Color(0xFF7C5CFC);
  static const Color violet2 = Color(0xFF9B7DFF);
  static const Color violet3 = Color(0xFFC4B0FF);
  static const Color violetGlow = Color(0x4D7C5CFC);
  static const Color violetSoft = Color(0x1F7C5CFC);

  // Text colors
  static const Color text1 = Color(0xFFF0EEFF);
  static const Color text2 = Color(0xFFA09DC0);
  static const Color text3 = Color(0xFF5C5980);

  // Glass colors
  static const Color glass = Color(0x14FFFFFF);
  static const Color glass2 = Color(0x1FFFFFFF);
  static const Color border = Color(0x17FFFFFF);
  static const Color border2 = Color(0x26FFFFFF);

  // Semantic colors
  static const Color success = Color(0xFF10B981);
  static const Color error = Color(0xFFEF4444);
  static const Color warning = Color(0xFFF59E0B);
  static const Color info = Color(0xFF3B82F6);

  // Source colors
  static const Color srcClaude = Color(0xFFA78BFA);
  static const Color srcSlack = Color(0xFFFB923C);
  static const Color srcGithub = Color(0xFF94A3B8);
  static const Color srcGmail = Color(0xFFF87171);
  static const Color srcInt = Color(0xFF34D399);

  // Backward-compatible aliases
  static const Color primary = violet;
  static const Color primaryLight = violet2;
  static const Color primaryDark = Color(0xFF5B3ED9);
  static const Color background = bg0;
  static const Color surface = bg2;
  static const Color surfaceLight = bg3;
  static const Color textPrimary = text1;
  static const Color textSecondary = text2;
  static const Color textTertiary = text3;
  static const Color claudeBadge = srcClaude;
  static const Color slackBadge = srcSlack;
  static const Color githubBadge = srcGithub;
  static const Color gmailBadge = srcGmail;
  static const Color glassBackground = Color(0x1AFFFFFF);
  static const Color glassBorder = Color(0x33FFFFFF);

  // Priority colors
  static const Color priorityHigh = Color(0xFFEF4444);
  static const Color priorityMedium = Color(0xFFF59E0B);
  static const Color priorityLow = Color(0xFF10B981);

  // Divider
  static const Color divider = Color(0xFF2A2A3E);

  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [violet2, violet],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient backgroundGradient = LinearGradient(
    colors: [bg0, bg1, bg2],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
}
