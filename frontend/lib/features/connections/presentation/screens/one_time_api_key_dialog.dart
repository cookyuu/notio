import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../providers/connection_providers.dart';

/// Dialog for displaying one-time API key
/// IMPORTANT: The API key is only displayed once and cannot be retrieved again
class OneTimeApiKeyDialog extends ConsumerStatefulWidget {
  final String apiKey;

  const OneTimeApiKeyDialog({
    super.key,
    required this.apiKey,
  });

  @override
  ConsumerState<OneTimeApiKeyDialog> createState() => _OneTimeApiKeyDialogState();
}

class _OneTimeApiKeyDialogState extends ConsumerState<OneTimeApiKeyDialog> {
  bool _isCopied = false;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.s16),
      ),
      title: Row(
        children: [
          const Icon(
            Icons.vpn_key,
            color: AppColors.warning,
            size: 28,
          ),
          const SizedBox(width: AppSpacing.s12),
          const Expanded(
            child: Text(
              'Your API Key',
              style: AppTextStyles.headlineMedium,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => _handleClose(context),
            tooltip: 'Close',
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Warning message
            Container(
              padding: const EdgeInsets.all(AppSpacing.s12),
              decoration: BoxDecoration(
                color: AppColors.warning.withAlpha(38),
                borderRadius: BorderRadius.circular(AppSpacing.s8),
                border: Border.all(
                  color: AppColors.warning.withAlpha(77),
                  width: 0.5,
                ),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.warning_amber_rounded,
                    color: AppColors.warning,
                    size: 20,
                  ),
                  const SizedBox(width: AppSpacing.s8),
                  Expanded(
                    child: Text(
                      'This is the only time you will see this key. Please copy it now.',
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.warning,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: AppSpacing.s24),

            // API Key display
            Text(
              'API Key',
              style: AppTextStyles.titleSmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.s8),

            Container(
              padding: const EdgeInsets.all(AppSpacing.s12),
              decoration: BoxDecoration(
                color: AppColors.background,
                borderRadius: BorderRadius.circular(AppSpacing.s8),
                border: Border.all(
                  color: AppColors.primary.withAlpha(77),
                  width: 1,
                ),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: SelectableText(
                      widget.apiKey,
                      style: AppTextStyles.bodySmall.copyWith(
                        fontFamily: 'monospace',
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: Icon(
                      _isCopied ? Icons.check : Icons.copy,
                      size: 20,
                      color: _isCopied ? AppColors.success : AppColors.primary,
                    ),
                    onPressed: _copyToClipboard,
                    tooltip: 'Copy to clipboard',
                  ),
                ],
              ),
            ),

            const SizedBox(height: AppSpacing.s24),

            // Usage example
            Text(
              'Usage Example (Claude Code)',
              style: AppTextStyles.titleSmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.s8),

            Container(
              padding: const EdgeInsets.all(AppSpacing.s12),
              decoration: BoxDecoration(
                color: AppColors.background,
                borderRadius: BorderRadius.circular(AppSpacing.s8),
                border: Border.all(
                  color: AppColors.divider,
                  width: 0.5,
                ),
              ),
              child: SelectableText(
                'NOTIO_WEBHOOK_API_KEY=${widget.apiKey}',
                style: AppTextStyles.bodySmall.copyWith(
                  fontFamily: 'monospace',
                  color: AppColors.textSecondary,
                ),
              ),
            ),

            const SizedBox(height: AppSpacing.s16),

            // Additional info
            Text(
              '💡 Add this to your .env file or export it in your shell configuration.',
              style: AppTextStyles.bodySmall.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _copyToClipboard() async {
    await Clipboard.setData(ClipboardData(text: widget.apiKey));
    setState(() {
      _isCopied = true;
    });

    // Reset copied state after 2 seconds
    Future.delayed(const Duration(seconds: 2), () {
      if (mounted) {
        setState(() {
          _isCopied = false;
        });
      }
    });
  }

  void _handleClose(BuildContext context) {
    // Discard the API key from state (safe to call even if already discarded)
    ref.read(oneTimeApiKeyProvider.notifier).discardApiKey();

    // Close dialog
    Navigator.of(context).pop();
  }

  @override
  void dispose() {
    // API key is discarded in _handleClose
    super.dispose();
  }
}
