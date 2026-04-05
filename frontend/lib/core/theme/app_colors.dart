import 'package:flutter/material.dart';

/// App color palette with dark mode and violet accent
class AppColors {
  AppColors._();

  // Primary Colors (Violet Accent)
  static const Color primary = Color(0xFF9D4EDD);
  static const Color primaryLight = Color(0xFFC77DFF);
  static const Color primaryDark = Color(0xFF7B2CBF);

  // Background Colors (Dark Mode)
  static const Color background = Color(0xFF0A0A0F);
  static const Color surface = Color(0xFF1A1A2E);
  static const Color surfaceLight = Color(0xFF252538);

  // Text Colors
  static const Color textPrimary = Color(0xFFFFFFFF);
  static const Color textSecondary = Color(0xFFB0B0C0);
  static const Color textTertiary = Color(0xFF808090);

  // Semantic Colors
  static const Color success = Color(0xFF10B981);
  static const Color error = Color(0xFFEF4444);
  static const Color warning = Color(0xFFF59E0B);
  static const Color info = Color(0xFF3B82F6);

  // Source Badge Colors
  static const Color claudeBadge = Color(0xFF9D4EDD);
  static const Color slackBadge = Color(0xFFE01E5A);
  static const Color githubBadge = Color(0xFF6CC644);
  static const Color gmailBadge = Color(0xFFEA4335);

  // Glass Effect Colors
  static const Color glassBackground = Color(0x1AFFFFFF);
  static const Color glassBorder = Color(0x33FFFFFF);

  // Priority Colors
  static const Color priorityHigh = Color(0xFFEF4444);
  static const Color priorityMedium = Color(0xFFF59E0B);
  static const Color priorityLow = Color(0xFF10B981);

  // Divider
  static const Color divider = Color(0xFF2A2A3E);

  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [primary, primaryDark],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient backgroundGradient = LinearGradient(
    colors: [background, surface],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );
}
