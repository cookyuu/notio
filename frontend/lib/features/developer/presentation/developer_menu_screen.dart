import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/features/notification/presentation/providers/notifications_notifier.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Developer menu screen for testing and debugging
class DeveloperMenuScreen extends HookConsumerWidget {
  const DeveloperMenuScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notifier = ref.read(notificationsProvider.notifier);
    final messageController = useTextEditingController(text: 'Test notification');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Developer Menu'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Test Notifications Section
            _buildSectionTitle('Test Notifications'),
            const SizedBox(height: 12),
            GlassCard(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TextField(
                      controller: messageController,
                      decoration: InputDecoration(
                        labelText: 'Notification Message',
                        labelStyle: AppTextStyles.bodyMedium.copyWith(
                          color: AppColors.textSecondary,
                        ),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide(color: AppColors.primary.withAlpha(77)),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide(color: AppColors.primary.withAlpha(77)),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide(color: AppColors.primary, width: 2),
                        ),
                      ),
                      style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
                      maxLines: 2,
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _buildNotificationButton(
                          context,
                          label: 'Claude (High)',
                          source: NotificationSource.claude,
                          priority: NotificationPriority.high,
                          message: messageController.text,
                          onPressed: () => _createTestNotification(
                            notifier,
                            messageController.text,
                            NotificationSource.claude,
                            NotificationPriority.high,
                          ),
                        ),
                        _buildNotificationButton(
                          context,
                          label: 'GitHub (Medium)',
                          source: NotificationSource.github,
                          priority: NotificationPriority.medium,
                          message: messageController.text,
                          onPressed: () => _createTestNotification(
                            notifier,
                            messageController.text,
                            NotificationSource.github,
                            NotificationPriority.medium,
                          ),
                        ),
                        _buildNotificationButton(
                          context,
                          label: 'Slack (Low)',
                          source: NotificationSource.slack,
                          priority: NotificationPriority.low,
                          message: messageController.text,
                          onPressed: () => _createTestNotification(
                            notifier,
                            messageController.text,
                            NotificationSource.slack,
                            NotificationPriority.low,
                          ),
                        ),
                        _buildNotificationButton(
                          context,
                          label: 'Gmail (Medium)',
                          source: NotificationSource.gmail,
                          priority: NotificationPriority.medium,
                          message: messageController.text,
                          onPressed: () => _createTestNotification(
                            notifier,
                            messageController.text,
                            NotificationSource.gmail,
                            NotificationPriority.medium,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Database Actions Section
            _buildSectionTitle('Database Actions'),
            const SizedBox(height: 12),
            GlassCard(
              child: Column(
                children: [
                  _buildActionTile(
                    title: 'Clear Notifications Cache',
                    subtitle: 'Remove all cached notifications',
                    icon: Icons.delete_outline,
                    onTap: () async {
                      await notifier.clearCache();
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: const Text('Notifications cache cleared'),
                            backgroundColor: AppColors.success,
                          ),
                        );
                      }
                    },
                  ),
                  const Divider(height: 1, color: AppColors.divider),
                  _buildActionTile(
                    title: 'Refresh Notifications',
                    subtitle: 'Fetch latest notifications from server',
                    icon: Icons.refresh,
                    onTap: () async {
                      await notifier.fetchNotifications(refresh: true);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: const Text('Notifications refreshed'),
                            backgroundColor: AppColors.success,
                          ),
                        );
                      }
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // App Info Section
            _buildSectionTitle('App Info'),
            const SizedBox(height: 12),
            GlassCard(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    _buildInfoRow('Version', '1.0.0'),
                    const SizedBox(height: 8),
                    _buildInfoRow('Build', 'Phase 4A - Drift Migration'),
                    const SizedBox(height: 8),
                    _buildInfoRow('Database', 'Drift (SQLite)'),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: AppTextStyles.headlineSmall.copyWith(color: AppColors.textPrimary),
    );
  }

  Widget _buildNotificationButton(
    BuildContext context, {
    required String label,
    required NotificationSource source,
    required NotificationPriority priority,
    required String message,
    required VoidCallback onPressed,
  }) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.primary.withAlpha(51),
        foregroundColor: AppColors.primary,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      child: Text(label),
    );
  }

  Widget _buildActionTile({
    required String title,
    required String subtitle,
    required IconData icon,
    required VoidCallback onTap,
  }) {
    return ListTile(
      leading: Icon(icon, color: AppColors.primary),
      title: Text(
        title,
        style: AppTextStyles.bodyMedium.copyWith(
          color: AppColors.textPrimary,
          fontWeight: FontWeight.w600,
        ),
      ),
      subtitle: Text(
        subtitle,
        style: AppTextStyles.caption.copyWith(color: AppColors.textSecondary),
      ),
      onTap: onTap,
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textSecondary),
        ),
        Text(
          value,
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }

  void _createTestNotification(
    NotificationsNotifier notifier,
    String message,
    NotificationSource source,
    NotificationPriority priority,
  ) {
    final notification = NotificationEntity(
      id: DateTime.now().millisecondsSinceEpoch,
      source: source,
      title: '${source.displayName} Test',
      body: message,
      priority: priority,
      isRead: false,
      createdAt: DateTime.now(),
    );

    // Add to local cache (this will trigger UI update)
    // Note: In a real app, this would be sent to the backend
    // For now, we'll just add it locally for testing
    notifier.addTestNotification(notification);
  }
}
