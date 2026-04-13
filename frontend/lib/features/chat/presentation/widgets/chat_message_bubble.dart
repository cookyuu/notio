import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';
import 'package:timeago/timeago.dart' as timeago;

/// Chat message bubble widget
class ChatMessageBubble extends StatelessWidget {
  final ChatMessageEntity message;

  const ChatMessageBubble({
    required this.message,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == MessageRole.user;

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.s16,
        vertical: AppSpacing.s8,
      ),
      child: Row(
        mainAxisAlignment:
            isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isUser) ...[
            _buildAvatar(isUser: false),
            const SizedBox(width: AppSpacing.s12),
          ],
          Flexible(
            child: Column(
              crossAxisAlignment:
                  isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
              children: [
                _buildMessageBubble(isUser),
                const SizedBox(height: AppSpacing.s4),
                Text(
                  timeago.format(message.createdAt, locale: 'ko'),
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
              ],
            ),
          ),
          if (isUser) ...[
            const SizedBox(width: AppSpacing.s12),
            _buildAvatar(isUser: true),
          ],
        ],
      ),
    );
  }

  Widget _buildAvatar({required bool isUser}) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: isUser
            ? AppColors.primaryGradient
            : const LinearGradient(
                colors: [AppColors.info, AppColors.primaryLight],
              ),
      ),
      child: Icon(
        isUser ? Icons.person : Icons.smart_toy_outlined,
        size: 18,
        color: AppColors.textPrimary,
      ),
    );
  }

  Widget _buildMessageBubble(bool isUser) {
    if (isUser) {
      // User message: solid violet background
      return Container(
        padding: const EdgeInsets.all(AppSpacing.s12),
        decoration: BoxDecoration(
          gradient: AppColors.primaryGradient,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Text(
          message.content,
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
      );
    } else {
      // AI message: glass effect
      return GlassCard(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s12),
          child: Text(
            message.content,
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
        ),
      );
    }
  }
}
