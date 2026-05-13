import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/channels/domain/entity/notification_channel_entity.dart';
import 'package:notio_app/features/channels/presentation/providers/channel_providers.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:timeago/timeago.dart' as timeago;

class ChannelsScreen extends ConsumerStatefulWidget {
  const ChannelsScreen({super.key});

  @override
  ConsumerState<ChannelsScreen> createState() => _ChannelsScreenState();
}

class _ChannelsScreenState extends ConsumerState<ChannelsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(channelNotifierProvider.notifier).load();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(channelNotifierProvider);

    ref.listen<ChannelState>(channelNotifierProvider, (previous, next) {
      if (next.successMessage != null &&
          next.successMessage != previous?.successMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.successMessage!),
            backgroundColor: AppColors.success,
          ),
        );
        ref.read(channelNotifierProvider.notifier).clearMessages();
      }
      if (next.error != null && next.error != previous?.error) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.error!),
            backgroundColor: AppColors.error,
          ),
        );
        ref.read(channelNotifierProvider.notifier).clearMessages();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('채널 관리'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
      ),
      body: _buildBody(state),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.push('${Routes.channels}/create'),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildBody(ChannelState state) {
    if (state.isLoading && state.channels.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.channels.isEmpty) {
      return _buildEmptyState();
    }

    return RefreshIndicator(
      onRefresh: () => ref.read(channelNotifierProvider.notifier).load(),
      child: ListView.builder(
        padding: const EdgeInsets.all(AppSpacing.s16),
        itemCount: state.channels.length,
        itemBuilder: (context, index) {
          return _ChannelCard(channel: state.channels[index]);
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.send_outlined,
            size: 64,
            color: AppColors.textTertiary,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            '등록된 채널이 없습니다',
            style: AppTextStyles.titleMedium
                .copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '+ 버튼을 눌러 채널을 추가하세요',
            style:
                AppTextStyles.bodySmall.copyWith(color: AppColors.textTertiary),
          ),
        ],
      ),
    );
  }
}

class _ChannelCard extends ConsumerWidget {
  const _ChannelCard({required this.channel});

  final NotificationChannelEntity channel;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isError = channel.status == ChannelStatusEnum.error;

    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.s12),
      child: Card(
        shape: isError
            ? RoundedRectangleBorder(
                side: const BorderSide(color: Colors.red, width: 1.5),
                borderRadius: BorderRadius.circular(12),
              )
            : null,
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  _ChannelTypeIcon(type: channel.channelType),
                  const SizedBox(width: AppSpacing.s12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(channel.displayName, style: AppTextStyles.bodyLarge),
                        const SizedBox(height: AppSpacing.s4),
                        _StatusBadge(status: channel.status),
                      ],
                    ),
                  ),
                  Switch(
                    value: channel.status == ChannelStatusEnum.active,
                    onChanged: channel.status == ChannelStatusEnum.error
                        ? null
                        : (val) => ref
                            .read(channelNotifierProvider.notifier)
                            .toggleStatus(channel.id, val),
                    activeColor: AppColors.primary,
                  ),
                  IconButton(
                    icon: const Icon(Icons.send_outlined),
                    tooltip: '테스트 전송',
                    onPressed: () => ref
                        .read(channelNotifierProvider.notifier)
                        .sendTest(channel.id),
                  ),
                ],
              ),
              if (isError && channel.lastError != null) ...[
                const SizedBox(height: AppSpacing.s8),
                Text(
                  '${channel.errorCount}회 실패: ${channel.lastError}',
                  style: const TextStyle(color: Colors.red, fontSize: 12),
                ),
              ],
              if (channel.lastDeliveredAt != null) ...[
                const SizedBox(height: AppSpacing.s4),
                Text(
                  '마지막 전달: ${timeago.format(channel.lastDeliveredAt!, locale: 'ko')}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _ChannelTypeIcon extends StatelessWidget {
  const _ChannelTypeIcon({required this.type});

  final ChannelTypeEnum type;

  @override
  Widget build(BuildContext context) {
    final (icon, color) = switch (type) {
      ChannelTypeEnum.slack => (Icons.chat_bubble, const Color(0xFF4A154B)),
      ChannelTypeEnum.telegram => (Icons.send, const Color(0xFF0088CC)),
      ChannelTypeEnum.discord => (Icons.headset, const Color(0xFF5865F2)),
    };

    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Icon(icon, size: 20, color: Colors.white),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final ChannelStatusEnum status;

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      ChannelStatusEnum.active => ('ACTIVE', AppColors.success),
      ChannelStatusEnum.paused => ('PAUSED', AppColors.textTertiary),
      ChannelStatusEnum.error => ('ERROR', AppColors.error),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withAlpha(30),
        border: Border.all(color: color.withAlpha(80)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
