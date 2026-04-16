import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../../domain/entity/connection_capability.dart';
import '../providers/connection_providers.dart';
import '../widgets/connection_status_badge.dart';
import '../widgets/connection_provider_icon.dart';
import '../widgets/connection_capability_chip.dart';
import 'one_time_api_key_dialog.dart';

/// Connection detail screen
class ConnectionDetailScreen extends ConsumerWidget {
  final int connectionId;

  const ConnectionDetailScreen({
    super.key,
    required this.connectionId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final connectionAsync = ref.watch(connectionDetailProvider(connectionId));
    final actionState = ref.watch(connectionActionsProvider);
    final oneTimeKeyState = ref.watch(oneTimeApiKeyProvider);

    // Show API key dialog if available (from rotate operation)
    if (oneTimeKeyState.isVisible && oneTimeKeyState.apiKey != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        // Save the API key before clearing the state
        final apiKey = oneTimeKeyState.apiKey!;

        // Immediately clear the state to prevent duplicate dialogs
        ref.read(oneTimeApiKeyProvider.notifier).discardApiKey();

        // Show the dialog
        showDialog(
          context: context,
          barrierDismissible: false,
          builder: (context) => OneTimeApiKeyDialog(apiKey: apiKey),
        );
      });
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Connection Details'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
      ),
      body: connectionAsync.when(
        data: (connection) => SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header Card
              Container(
                padding: const EdgeInsets.all(AppSpacing.s20),
                decoration: BoxDecoration(
                  color: AppColors.glassBackground,
                  borderRadius: BorderRadius.circular(AppSpacing.s16),
                  border: Border.all(
                    color: AppColors.glassBorder,
                    width: 0.5,
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        ConnectionProviderIcon.buildIcon(connection.provider, size: 40),
                        const SizedBox(width: AppSpacing.s16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                connection.displayName,
                                style: AppTextStyles.headlineMedium,
                              ),
                              if (connection.accountLabel != null) ...[
                                const SizedBox(height: AppSpacing.s4),
                                Text(
                                  connection.accountLabel!,
                                  style: AppTextStyles.bodyMedium.copyWith(
                                    color: AppColors.textSecondary,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                        ConnectionStatusBadge(status: connection.status),
                      ],
                    ),

                    const SizedBox(height: AppSpacing.s20),

                    // Key Preview
                    if (connection.keyPreview != null) ...[
                      Text(
                        'Key Preview',
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
                        ),
                        child: Text(
                          connection.keyPreview!,
                          style: AppTextStyles.bodyMedium.copyWith(
                            fontFamily: 'monospace',
                            color: AppColors.textTertiary,
                          ),
                        ),
                      ),
                      const SizedBox(height: AppSpacing.s20),
                    ],

                    // Capabilities
                    if (connection.capabilities.isNotEmpty) ...[
                      Text(
                        'Capabilities',
                        style: AppTextStyles.titleSmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.s8),
                      Wrap(
                        spacing: AppSpacing.s8,
                        runSpacing: AppSpacing.s8,
                        children: connection.capabilities
                            .map((cap) => ConnectionCapabilityChip(capability: cap))
                            .toList(),
                      ),
                      const SizedBox(height: AppSpacing.s20),
                    ],

                    // Timestamps
                    _buildInfoRow('Created', timeago.format(connection.createdAt)),
                    if (connection.lastUsedAt != null)
                      _buildInfoRow('Last Used', timeago.format(connection.lastUsedAt!)),
                  ],
                ),
              ),

              const SizedBox(height: AppSpacing.s24),

              // Actions Section
              Text(
                'Actions',
                style: AppTextStyles.titleMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: AppSpacing.s12),

              // Test Connection Button
              if (connection.capabilities.contains(ConnectionCapability.testMessage))
                _buildActionButton(
                  context: context,
                  label: 'Test Connection',
                  icon: Icons.send_outlined,
                  color: AppColors.info,
                  isLoading: actionState.isTesting,
                  onPressed: () => _handleTest(ref, connection.id),
                ),

              // Refresh Token Button
              if (connection.capabilities.contains(ConnectionCapability.refreshToken) &&
                  connection.authType == ConnectionAuthType.oauth)
                _buildActionButton(
                  context: context,
                  label: 'Refresh OAuth Token',
                  icon: Icons.refresh,
                  color: AppColors.primary,
                  isLoading: actionState.isRefreshing,
                  onPressed: () => _handleRefresh(ref, connection.id),
                ),

              // Rotate API Key Button
              if (connection.capabilities.contains(ConnectionCapability.rotateKey) &&
                  connection.authType == ConnectionAuthType.apiKey)
                _buildActionButton(
                  context: context,
                  label: 'Rotate API Key',
                  icon: Icons.vpn_key_outlined,
                  color: AppColors.warning,
                  isLoading: actionState.isRotating,
                  onPressed: () => _handleRotateKey(context, ref, connection.id),
                ),

              // Disconnect Button
              _buildActionButton(
                context: context,
                label: 'Disconnect',
                icon: Icons.link_off,
                color: AppColors.error,
                isLoading: actionState.isDeleting,
                onPressed: () => _handleDisconnect(context, ref, connection.id),
              ),

              // Error/Success Messages
              if (actionState.errorMessage != null) ...[
                const SizedBox(height: AppSpacing.s16),
                _buildMessageBox(
                  message: actionState.errorMessage!,
                  isError: true,
                ),
              ],

              if (actionState.successMessage != null) ...[
                const SizedBox(height: AppSpacing.s16),
                _buildMessageBox(
                  message: actionState.successMessage!,
                  isError: false,
                ),
              ],
            ],
          ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 64, color: AppColors.error),
              const SizedBox(height: AppSpacing.s16),
              Text(
                'Error loading connection',
                style: AppTextStyles.titleMedium,
              ),
              const SizedBox(height: AppSpacing.s8),
              Text(
                error.toString(),
                style: AppTextStyles.bodySmall.copyWith(
                  color: AppColors.textTertiary,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.s8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          Text(
            value,
            style: AppTextStyles.bodyMedium,
          ),
        ],
      ),
    );
  }

  Widget _buildActionButton({
    required BuildContext context,
    required String label,
    required IconData icon,
    required Color color,
    required bool isLoading,
    required VoidCallback onPressed,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.s12),
      child: SizedBox(
        width: double.infinity,
        child: ElevatedButton.icon(
          onPressed: isLoading ? null : onPressed,
          icon: isLoading
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(AppColors.textPrimary),
                  ),
                )
              : Icon(icon),
          label: Text(label),
          style: ElevatedButton.styleFrom(
            backgroundColor: color,
            foregroundColor: AppColors.textPrimary,
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.s16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(AppSpacing.s12),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMessageBox({required String message, required bool isError}) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.s12),
      decoration: BoxDecoration(
        color: isError
            ? AppColors.error.withAlpha(38)
            : AppColors.success.withAlpha(38),
        borderRadius: BorderRadius.circular(AppSpacing.s8),
      ),
      child: Row(
        children: [
          Icon(
            isError ? Icons.error_outline : Icons.check_circle_outline,
            color: isError ? AppColors.error : AppColors.success,
            size: 20,
          ),
          const SizedBox(width: AppSpacing.s8),
          Expanded(
            child: Text(
              message,
              style: AppTextStyles.bodySmall.copyWith(
                color: isError ? AppColors.error : AppColors.success,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _handleTest(WidgetRef ref, int id) async {
    await ref.read(connectionActionsProvider.notifier).testConnection(id);
  }

  Future<void> _handleRefresh(WidgetRef ref, int id) async {
    await ref.read(connectionActionsProvider.notifier).refreshConnection(id);
  }

  Future<void> _handleRotateKey(BuildContext context, WidgetRef ref, int id) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Rotate API Key'),
        content: const Text(
          'This will invalidate your current API key and generate a new one. '
          'You will need to update your applications with the new key. Continue?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(
              'Rotate',
              style: TextStyle(color: AppColors.warning),
            ),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await ref.read(connectionActionsProvider.notifier).rotateKey(id);
    }
  }

  Future<void> _handleDisconnect(BuildContext context, WidgetRef ref, int id) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Disconnect'),
        content: const Text(
          'Are you sure you want to disconnect this connection? '
          'This action cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(
              'Disconnect',
              style: TextStyle(color: AppColors.error),
            ),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await ref.read(connectionActionsProvider.notifier).deleteConnection(id);
      if (context.mounted) {
        context.pop(); // Go back to connections list
      }
    }
  }
}
