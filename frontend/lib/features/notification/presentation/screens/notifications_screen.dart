import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/network/network_status_provider.dart';
import 'package:notio_app/core/network/sync_service.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/notification/presentation/providers/notifications_notifier.dart';
import 'package:notio_app/features/notification/presentation/widgets/notification_card.dart';
import 'package:notio_app/features/notification/presentation/widgets/notification_detail_modal.dart';

/// Notifications screen
class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() =>
      _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  final ScrollController _scrollController = ScrollController();
  int? _loadingNotificationId;

  @override
  void initState() {
    super.initState();
    // Load notifications on init
    Future.microtask(() {
      ref
          .read(notificationsProvider.notifier)
          .fetchNotifications(refresh: true);
    });

    // Setup scroll listener for pagination
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent * 0.8) {
      ref.read(notificationsProvider.notifier).loadMore();
    }
  }

  Future<void> _onRefresh() async {
    await ref
        .read(notificationsProvider.notifier)
        .fetchNotifications(refresh: true);
    ref.invalidate(unreadCountProvider);
  }

  Future<void> _openNotificationDetail(int notificationId) async {
    if (_loadingNotificationId != null) {
      return;
    }

    setState(() {
      _loadingNotificationId = notificationId;
    });

    try {
      final detail = await ref
          .read(notificationsProvider.notifier)
          .fetchNotificationDetail(notificationId);

      if (!mounted) {
        return;
      }

      await showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (context) => NotificationDetailModal(
          notification: detail,
        ),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          backgroundColor: AppColors.bg3,
          content: Text(
            '상세 알림을 불러오지 못했습니다. ${error.toString()}',
            style: AppTextStyles.bodySmall.copyWith(
              color: AppColors.text1,
            ),
          ),
        ),
      );
    } finally {
      if (mounted) {
        setState(() {
          _loadingNotificationId = null;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(notificationsProvider);
    final unreadCountAsync = ref.watch(unreadCountProvider);
    final networkStatus = ref.watch(networkStatusProvider);
    final syncState = ref.watch(syncServiceProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        title: Row(
          children: [
            Text(
              'Notio',
              style: AppTextStyles.headlineMedium.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(width: AppSpacing.s8),
            unreadCountAsync.when(
              data: (count) => count > 0
                  ? Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppSpacing.s8,
                        vertical: AppSpacing.s4,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.primary,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        '$count',
                        style: AppTextStyles.labelSmall.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  : const SizedBox.shrink(),
              loading: () => const SizedBox.shrink(),
              error: (_, __) => const SizedBox.shrink(),
            ),
          ],
        ),
        actions: [
          // Network status indicator
          _buildNetworkStatusIndicator(networkStatus, syncState),
          const SizedBox(width: AppSpacing.s8),
          IconButton(
            icon: const Icon(Icons.done_all),
            onPressed: state.notifications.isNotEmpty
                ? () {
                    ref.read(notificationsProvider.notifier).markAllAsRead();
                    ref.invalidate(unreadCountProvider);
                  }
                : null,
            tooltip: '전체 읽음',
          ),
        ],
      ),
      body: Column(
        children: [
          // Offline mode banner
          if (networkStatus == NetworkStatus.offline)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.s16,
                vertical: AppSpacing.s12,
              ),
              color: AppColors.warning.withValues(alpha: 0.15),
              child: Row(
                children: [
                  const Icon(
                    Icons.cloud_off,
                    size: 16,
                    color: AppColors.warning,
                  ),
                  const SizedBox(width: AppSpacing.s8),
                  Expanded(
                    child: Text(
                      '오프라인 모드 - 로컬에 저장된 데이터를 표시하고 있습니다',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: AppColors.warning,
                      ),
                    ),
                  ),
                ],
              ),
            ),

          // Sync success banner
          if (syncState.status == SyncStatus.success &&
              syncState.lastSyncTime != null &&
              DateTime.now().difference(syncState.lastSyncTime!).inSeconds < 5)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.s16,
                vertical: AppSpacing.s12,
              ),
              color: AppColors.success.withValues(alpha: 0.15),
              child: Row(
                children: [
                  const Icon(
                    Icons.check_circle,
                    size: 16,
                    color: AppColors.success,
                  ),
                  const SizedBox(width: AppSpacing.s8),
                  Expanded(
                    child: Text(
                      '데이터 동기화 완료',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: AppColors.success,
                      ),
                    ),
                  ),
                ],
              ),
            ),

          // Filter chips
          _buildFilterChips(),
          const SizedBox(height: AppSpacing.s8),

          // Notifications list
          Expanded(
            child: state.isLoading && state.notifications.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : state.error != null && state.notifications.isEmpty
                    ? _buildError(state.error!)
                    : state.notifications.isEmpty
                        ? _buildEmptyState()
                        : RefreshIndicator(
                            onRefresh: _onRefresh,
                            color: AppColors.primary,
                            backgroundColor: AppColors.surface,
                            child: ListView.builder(
                              controller: _scrollController,
                              itemCount: state.notifications.length +
                                  (state.hasMore ? 1 : 0),
                              itemBuilder: (context, index) {
                                if (index >= state.notifications.length) {
                                  return const Center(
                                    child: Padding(
                                      padding: EdgeInsets.all(AppSpacing.s16),
                                      child: CircularProgressIndicator(),
                                    ),
                                  );
                                }

                                final notification = state.notifications[index];
                                return NotificationCard(
                                  notification: notification,
                                  isLoading:
                                      _loadingNotificationId == notification.id,
                                  onTap: () =>
                                      _openNotificationDetail(notification.id),
                                  onMarkAsRead: () {
                                    ref
                                        .read(notificationsProvider.notifier)
                                        .markAsRead(notification.id);
                                    ref.invalidate(unreadCountProvider);
                                  },
                                  onDelete: () {
                                    ref
                                        .read(notificationsProvider.notifier)
                                        .deleteNotification(notification.id);
                                    ref.invalidate(unreadCountProvider);
                                  },
                                );
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildNetworkStatusIndicator(
    NetworkStatus networkStatus,
    SyncState syncState,
  ) {
    // Show syncing indicator
    if (syncState.status == SyncStatus.syncing) {
      return const Padding(
        padding: EdgeInsets.all(AppSpacing.s12),
        child: SizedBox(
          width: 20,
          height: 20,
          child: CircularProgressIndicator(
            strokeWidth: 2,
            valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
          ),
        ),
      );
    }

    // Show network status icon
    return Tooltip(
      message: networkStatus == NetworkStatus.online
          ? '온라인'
          : networkStatus == NetworkStatus.offline
              ? '오프라인 (로컬 데이터 사용 중)'
              : '네트워크 상태 확인 중',
      child: Icon(
        networkStatus == NetworkStatus.online
            ? Icons.cloud_done
            : networkStatus == NetworkStatus.offline
                ? Icons.cloud_off
                : Icons.cloud_queue,
        color: networkStatus == NetworkStatus.online
            ? AppColors.success
            : networkStatus == NetworkStatus.offline
                ? AppColors.warning
                : AppColors.textTertiary,
        size: 20,
      ),
    );
  }

  Widget _buildFilterChips() {
    final state = ref.watch(notificationsProvider);
    final filters = [
      (null, '전체'),
      (NotificationSource.claude, 'Claude'),
      (NotificationSource.slack, 'Slack'),
      (NotificationSource.github, 'GitHub'),
      (NotificationSource.gmail, 'Gmail'),
    ];

    return SizedBox(
      height: 50,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.s16),
        itemCount: filters.length,
        separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.s8),
        itemBuilder: (context, index) {
          final filter = filters[index];
          final isSelected = state.selectedSource == filter.$1;

          return FilterChip(
            label: Text(filter.$2),
            selected: isSelected,
            onSelected: (_) {
              ref
                  .read(notificationsProvider.notifier)
                  .setSourceFilter(filter.$1);
            },
            backgroundColor: AppColors.surface,
            selectedColor: AppColors.primary,
            labelStyle: AppTextStyles.labelMedium.copyWith(
              color: isSelected ? Colors.white : AppColors.textSecondary,
            ),
            side: BorderSide(
              color: isSelected ? AppColors.primary : AppColors.divider,
            ),
          );
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.notifications_off_outlined,
            size: 64,
            color: AppColors.textTertiary,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            '알림이 없습니다',
            style: AppTextStyles.headlineSmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '새로운 알림이 도착하면 여기에 표시됩니다',
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildError(String error) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.error_outline,
            size: 64,
            color: AppColors.error,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            '오류가 발생했습니다',
            style: AppTextStyles.headlineSmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            error,
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.s16),
          ElevatedButton(
            onPressed: _onRefresh,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
            ),
            child: const Text('다시 시도'),
          ),
        ],
      ),
    );
  }
}
