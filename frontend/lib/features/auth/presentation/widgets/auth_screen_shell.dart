import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

class AuthScreenShell extends StatelessWidget {
  const AuthScreenShell({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.child,
    this.bottomAction,
    super.key,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final Widget child;
  final Widget? bottomAction;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(gradient: AppColors.backgroundGradient),
        child: Stack(
          children: [
            const _AuthBackdrop(),
            SafeArea(
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(AppSpacing.s24),
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 520),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 88,
                          height: 88,
                          decoration: const BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: AppColors.primaryGradient,
                            boxShadow: [
                              BoxShadow(
                                color: AppColors.violetGlow,
                                blurRadius: 32,
                                spreadRadius: 6,
                              ),
                            ],
                          ),
                          child: Icon(
                            icon,
                            color: AppColors.text1,
                            size: 38,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.s24),
                        Text(
                          title,
                          textAlign: TextAlign.center,
                          style: AppTextStyles.displayMedium,
                        ),
                        const SizedBox(height: AppSpacing.s8),
                        Text(
                          subtitle,
                          textAlign: TextAlign.center,
                          style: AppTextStyles.bodyMedium.copyWith(
                            color: AppColors.text2,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.s32),
                        GlassCard(
                          padding: const EdgeInsets.all(AppSpacing.s24),
                          borderRadius: 28,
                          child: child,
                        ),
                        if (bottomAction != null) ...[
                          const SizedBox(height: AppSpacing.s20),
                          bottomAction!,
                        ],
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class AuthStatusCard extends StatelessWidget {
  const AuthStatusCard({
    required this.message,
    required this.icon,
    required this.color,
    super.key,
  });

  final String message;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.s12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: color),
          const SizedBox(width: AppSpacing.s8),
          Expanded(
            child: Text(
              message,
              style: AppTextStyles.bodySmall.copyWith(color: AppColors.text1),
            ),
          ),
        ],
      ),
    );
  }
}

class AuthSecondaryAction extends StatelessWidget {
  const AuthSecondaryAction({
    required this.label,
    required this.onPressed,
    super.key,
  });

  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      child: Text(
        label,
        style: AppTextStyles.labelLarge.copyWith(color: AppColors.violet3),
      ),
    );
  }
}

class _AuthBackdrop extends StatelessWidget {
  const _AuthBackdrop();

  @override
  Widget build(BuildContext context) {
    return const IgnorePointer(
      child: Stack(
        children: [
          Positioned(
            top: -80,
            left: -30,
            child: _GlowBubble(
              size: 220,
              color: AppColors.violetGlow,
            ),
          ),
          Positioned(
            right: -40,
            bottom: 60,
            child: _GlowBubble(
              size: 180,
              color: AppColors.violetSoft,
            ),
          ),
        ],
      ),
    );
  }
}

class _GlowBubble extends StatelessWidget {
  const _GlowBubble({
    required this.size,
    required this.color,
  });

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(
          colors: [
            color,
            color.withValues(alpha: 0),
          ],
        ),
      ),
    );
  }
}
