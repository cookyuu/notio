import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_colors.dart';

/// Glassmorphism card widget
class GlassCard extends StatelessWidget {
  const GlassCard({
    required this.child,
    this.blur = 10.0,
    this.opacity = 0.1,
    this.borderRadius = 16.0,
    this.padding,
    this.border = true,
    super.key,
  });

  final Widget child;
  final double blur;
  final double opacity;
  final double borderRadius;
  final EdgeInsetsGeometry? padding;
  final bool border;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
        child: Container(
          decoration: BoxDecoration(
            color: AppColors.glassBackground,
            borderRadius: BorderRadius.circular(borderRadius),
            border: border
                ? Border.all(
                    color: AppColors.glassBorder,
                    width: 1,
                  )
                : null,
          ),
          padding: padding,
          child: child,
        ),
      ),
    );
  }
}
