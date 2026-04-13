import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/services/local_notification_service.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/main.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

/// Developer menu screen for testing features
class DeveloperMenuScreen extends ConsumerWidget {
  const DeveloperMenuScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notificationService = ref.read(localNotificationServiceProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Developer Menu'),
        backgroundColor: AppColors.surface,
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.s16),
        children: [
          // Local Notifications Section
          const _SectionHeader(title: 'Local Notifications'),
          const SizedBox(height: AppSpacing.s12),

          _TestNotificationCard(
            title: 'Urgent Slack Notification',
            description: 'Send a high-priority Slack notification',
            icon: Icons.notification_important,
            color: AppColors.error,
            onTap: () => _sendTestNotification(
              notificationService,
              priority: NotificationPriority.urgent,
              source: NotificationSource.slack,
              title: 'Urgent: Production Issue',
              body: 'Server CPU usage is above 90%. Immediate action required!',
            ),
          ),

          const SizedBox(height: AppSpacing.s12),

          _TestNotificationCard(
            title: 'GitHub Pull Request',
            description: 'Send a medium-priority GitHub notification',
            icon: Icons.code,
            color: AppColors.primary,
            onTap: () => _sendTestNotification(
              notificationService,
              priority: NotificationPriority.high,
              source: NotificationSource.github,
              title: 'New Pull Request',
              body: 'John Doe opened a PR: "Fix authentication bug"',
            ),
          ),

          const SizedBox(height: AppSpacing.s12),

          _TestNotificationCard(
            title: 'Claude Code Update',
            description: 'Send a Claude notification',
            icon: Icons.smart_toy,
            color: AppColors.info,
            onTap: () => _sendTestNotification(
              notificationService,
              priority: NotificationPriority.medium,
              source: NotificationSource.claude,
              title: 'Claude Code',
              body: 'Your code analysis is complete. 3 issues found.',
            ),
          ),

          const SizedBox(height: AppSpacing.s12),

          _TestNotificationCard(
            title: 'Gmail Low Priority',
            description: 'Send a low-priority Gmail notification',
            icon: Icons.email,
            color: AppColors.textSecondary,
            onTap: () => _sendTestNotification(
              notificationService,
              priority: NotificationPriority.low,
              source: NotificationSource.gmail,
              title: 'New Email',
              body: 'Newsletter: Weekly Developer Digest',
            ),
          ),

          const SizedBox(height: AppSpacing.s24),

          // Other Actions Section
          const _SectionHeader(title: 'Actions'),
          const SizedBox(height: AppSpacing.s12),

          _ActionCard(
            title: 'Cancel All Notifications',
            description: 'Clear all active notifications',
            icon: Icons.clear_all,
            color: AppColors.error,
            onTap: () async {
              await notificationService.cancelAllNotifications();
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('All notifications cleared'),
                    duration: Duration(seconds: 2),
                  ),
                );
              }
            },
          ),

          const SizedBox(height: AppSpacing.s12),

          _ActionCard(
            title: 'Request Permissions',
            description: 'Request notification permissions (iOS)',
            icon: Icons.security,
            color: AppColors.primary,
            onTap: () async {
              final granted = await notificationService.requestPermissions();
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(
                      granted == true
                          ? 'Permissions granted'
                          : 'Permissions denied or already granted',
                    ),
                    duration: const Duration(seconds: 2),
                  ),
                );
              }
            },
          ),

          const SizedBox(height: AppSpacing.s12),

          _ActionCard(
            title: 'View Active Notifications',
            description: 'Show currently active notifications (Android)',
            icon: Icons.list,
            color: AppColors.info,
            onTap: () async {
              await notificationService.getActiveNotifications();
              if (context.mounted) {
                showDialog(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('Active Notifications'),
                    content: const Text(
                      'Active notifications are shown in the system tray.',
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.of(context).pop(),
                        child: const Text('OK'),
                      ),
                    ],
                  ),
                );
              }
            },
          ),
        ],
      ),
    );
  }

  void _sendTestNotification(
    LocalNotificationService service, {
    required NotificationPriority priority,
    required NotificationSource source,
    required String title,
    required String body,
  }) {
    service.showNotification(
      id: DateTime.now().millisecondsSinceEpoch ~/ 1000,
      title: title,
      body: body,
      priority: priority,
      source: source,
      payload: 'test_notification',
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;

  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: AppSpacing.s8),
      child: Text(
        title,
        style: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.bold,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}

class _TestNotificationCard extends StatelessWidget {
  final String title;
  final String description;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  const _TestNotificationCard({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(AppSpacing.s12),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(
                  icon,
                  color: color,
                  size: 24,
                ),
              ),
              const SizedBox(width: AppSpacing.s16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.s4),
                    Text(
                      description,
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.send,
                color: AppColors.textSecondary,
                size: 20,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ActionCard extends StatelessWidget {
  final String title;
  final String description;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  const _ActionCard({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s16),
          child: Row(
            children: [
              Icon(
                icon,
                color: color,
                size: 28,
              ),
              const SizedBox(width: AppSpacing.s16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.s4),
                    Text(
                      description,
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right,
                color: AppColors.textSecondary,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
