import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_session_notifier.dart';
import 'package:notio_app/features/settings/presentation/providers/settings_providers.dart';

/// Settings screen (Phase 3)
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider);
    final settingsNotifier = ref.read(settingsProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.s16),
        children: [
          // Appearance Section
          _buildSectionHeader('외관'),
          _buildDarkModeToggle(context, ref, settings.isDarkMode, settingsNotifier),
          const Divider(height: 32, color: AppColors.divider),

          // Notifications Section
          _buildSectionHeader('알림'),
          _buildPushNotificationToggle(
            context,
            ref,
            settings.isPushEnabled,
            settingsNotifier,
          ),
          const SizedBox(height: AppSpacing.s16),
          _buildDefaultFilterSetting(
            context,
            ref,
            settings.defaultFilter,
            settingsNotifier,
          ),
          const Divider(height: 32, color: AppColors.divider),

          // Connections Section
          _buildSectionHeader('연동'),
          _buildConnectionsManagementButton(context),
          const Divider(height: 32, color: AppColors.divider),

          // Account Section
          _buildSectionHeader('계정'),
          _buildLogoutButton(context, ref),
          const Divider(height: 32, color: AppColors.divider),

          // About Section
          _buildSectionHeader('정보'),
          _buildVersionInfo(),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(
        left: AppSpacing.s8,
        bottom: AppSpacing.s12,
      ),
      child: Text(
        title,
        style: AppTextStyles.headlineSmall.copyWith(
          color: AppColors.primary,
        ),
      ),
    );
  }

  Widget _buildDarkModeToggle(
    BuildContext context,
    WidgetRef ref,
    bool isDarkMode,
    settingsNotifier,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(color: AppColors.divider),
      ),
      child: SwitchListTile(
        title: const Text(
          '다크 모드',
          style: AppTextStyles.bodyLarge,
        ),
        subtitle: Text(
          '어두운 테마 사용',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        value: isDarkMode,
        onChanged: (value) async {
          await settingsNotifier.toggleDarkMode();
        },
        activeTrackColor: AppColors.primary.withAlpha(128),
        activeThumbColor: AppColors.primary,
        secondary: const Icon(
          Icons.dark_mode,
          color: AppColors.primary,
        ),
      ),
    );
  }

  Widget _buildPushNotificationToggle(
    BuildContext context,
    WidgetRef ref,
    bool isPushEnabled,
    settingsNotifier,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(color: AppColors.divider),
      ),
      child: SwitchListTile(
        title: const Text(
          '푸시 알림',
          style: AppTextStyles.bodyLarge,
        ),
        subtitle: Text(
          '새 알림을 즉시 받기',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        value: isPushEnabled,
        onChanged: (value) async {
          await settingsNotifier.togglePushNotification();
        },
        activeTrackColor: AppColors.primary.withAlpha(128),
        activeThumbColor: AppColors.primary,
        secondary: const Icon(
          Icons.notifications_active,
          color: AppColors.primary,
        ),
      ),
    );
  }

  Widget _buildDefaultFilterSetting(
    BuildContext context,
    WidgetRef ref,
    NotificationSource? defaultFilter,
    settingsNotifier,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(color: AppColors.divider),
      ),
      child: ListTile(
        leading: const Icon(
          Icons.filter_list,
          color: AppColors.primary,
        ),
        title: const Text(
          '기본 필터',
          style: AppTextStyles.bodyLarge,
        ),
        subtitle: Text(
          defaultFilter?.displayName ?? '전체 보기',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          _showFilterDialog(context, ref, defaultFilter, settingsNotifier);
        },
      ),
    );
  }

  void _showFilterDialog(
    BuildContext context,
    WidgetRef ref,
    NotificationSource? currentFilter,
    settingsNotifier,
  ) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('기본 필터 설정'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildFilterOption(
              context,
              null,
              '전체 보기',
              currentFilter == null,
              () async {
                await settingsNotifier.setDefaultFilter(null);
                if (context.mounted) Navigator.pop(context);
              },
            ),
            ...NotificationSource.values.map(
              (source) => _buildFilterOption(
                context,
                source,
                source.displayName,
                currentFilter == source,
                () async {
                  await settingsNotifier.setDefaultFilter(source);
                  if (context.mounted) Navigator.pop(context);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterOption(
    BuildContext context,
    NotificationSource? source,
    String label,
    bool isSelected,
    VoidCallback onTap,
  ) {
    return ListTile(
      title: Text(label),
      trailing: isSelected
          ? const Icon(Icons.check, color: AppColors.primary)
          : null,
      onTap: onTap,
    );
  }

  Widget _buildConnectionsManagementButton(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(color: AppColors.divider),
      ),
      child: ListTile(
        leading: const Icon(
          Icons.hub_outlined,
          color: AppColors.primary,
        ),
        title: const Text(
          '연동 관리',
          style: AppTextStyles.bodyLarge,
        ),
        subtitle: Text(
          'Claude, Slack, Gmail 등 외부 서비스 연동',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        trailing: const Icon(
          Icons.chevron_right,
          color: AppColors.textSecondary,
        ),
        onTap: () {
          context.push('/settings/connections');
        },
      ),
    );
  }

  Widget _buildLogoutButton(BuildContext context, WidgetRef ref) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(color: AppColors.divider),
      ),
      child: ListTile(
        leading: const Icon(
          Icons.logout,
          color: AppColors.error,
        ),
        title: const Text(
          '로그아웃',
          style: AppTextStyles.bodyLarge,
        ),
        subtitle: Text(
          '계정에서 로그아웃',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          _showLogoutConfirmDialog(context, ref);
        },
      ),
    );
  }

  void _showLogoutConfirmDialog(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('정말 로그아웃하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(context);
              await ref.read(authSessionNotifierProvider.notifier).clearSession();
            },
            child: const Text(
              '로그아웃',
              style: TextStyle(color: AppColors.error),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVersionInfo() {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.s12),
        border: Border.all(color: AppColors.divider),
      ),
      child: ListTile(
        leading: const Icon(
          Icons.info_outline,
          color: AppColors.primary,
        ),
        title: const Text(
          '버전 정보',
          style: AppTextStyles.bodyLarge,
        ),
        subtitle: Text(
          'v1.0.0 (Phase 4A)',
          style: AppTextStyles.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}
