import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Streaming message bubble (for SSE responses)
class StreamingMessageBubble extends StatefulWidget {
  final String content;

  const StreamingMessageBubble({
    required this.content,
    super.key,
  });

  @override
  State<StreamingMessageBubble> createState() => _StreamingMessageBubbleState();
}

class _StreamingMessageBubbleState extends State<StreamingMessageBubble>
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.s16,
        vertical: AppSpacing.s8,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildAvatar(),
          const SizedBox(width: AppSpacing.s12),
          Flexible(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                GlassCard(
                  child: Padding(
                    padding: const EdgeInsets.all(AppSpacing.s12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (widget.content.isNotEmpty) ...[
                          Text(
                            widget.content,
                            style: AppTextStyles.bodyMedium.copyWith(
                              color: AppColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.s8),
                        ],
                        _buildTypingIndicator(),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvatar() {
    return Container(
      width: 32,
      height: 32,
      decoration: const BoxDecoration(
        shape: BoxShape.circle,
        gradient: LinearGradient(
          colors: [AppColors.info, AppColors.primaryLight],
        ),
      ),
      child: const Icon(
        Icons.smart_toy_outlined,
        size: 18,
        color: AppColors.textPrimary,
      ),
    );
  }

  Widget _buildTypingIndicator() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _buildDot(delay: 0.0),
        const SizedBox(width: 4),
        _buildDot(delay: 0.2),
        const SizedBox(width: 4),
        _buildDot(delay: 0.4),
      ],
    );
  }

  Widget _buildDot({required double delay}) {
    return AnimatedBuilder(
      animation: _animationController,
      builder: (context, child) {
        // Calculate opacity with delay
        final value = (_animationController.value - delay) % 1.0;
        final opacity = (value < 0.5) ? value * 2 : (1.0 - value) * 2;

        return Opacity(
          opacity: opacity.clamp(0.3, 1.0),
          child: Container(
            width: 6,
            height: 6,
            decoration: const BoxDecoration(
              color: AppColors.primary,
              shape: BoxShape.circle,
            ),
          ),
        );
      },
    );
  }
}
