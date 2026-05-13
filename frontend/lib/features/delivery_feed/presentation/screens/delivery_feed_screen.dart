import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/presentation/providers/delivery_feed_notifier.dart';
import 'package:notio_app/features/delivery_feed/presentation/providers/delivery_feed_state.dart';
import 'package:notio_app/features/delivery_feed/presentation/widgets/channel_filter_chips.dart';
import 'package:notio_app/features/delivery_feed/presentation/widgets/delivery_bubble.dart';

class DeliveryFeedScreen extends ConsumerStatefulWidget {
  const DeliveryFeedScreen({super.key});

  @override
  ConsumerState<DeliveryFeedScreen> createState() => _DeliveryFeedScreenState();
}

class _DeliveryFeedScreenState extends ConsumerState<DeliveryFeedScreen> {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(deliveryFeedNotifierProvider.notifier).load();
    });
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
      ref.read(deliveryFeedNotifierProvider.notifier).loadMore();
    }
  }

  Future<void> _onRefresh() async {
    await ref.read(deliveryFeedNotifierProvider.notifier).load();
  }

  void _onFilterSelected(ChannelTypeEnum? channelType) {
    ref.read(deliveryFeedNotifierProvider.notifier).setFilter(channelType);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(deliveryFeedNotifierProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        title: Text(
          'Deliveries',
          style: AppTextStyles.headlineMedium.copyWith(
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: Column(
        children: [
          ChannelFilterChips(
            selected: state.filter,
            onSelected: _onFilterSelected,
          ),
          const SizedBox(height: AppSpacing.s8),
          Expanded(
            child: _buildBody(state),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(DeliveryFeedState state) {
    if (state.isLoading && state.items.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.error != null && state.items.isEmpty) {
      return _buildError(state.error!);
    }

    if (state.items.isEmpty) {
      return const _EmptyFeedState();
    }

    return RefreshIndicator(
      onRefresh: _onRefresh,
      color: AppColors.primary,
      backgroundColor: AppColors.surface,
      child: ListView.builder(
        controller: _scrollController,
        itemCount: state.items.length + (state.isLoadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index >= state.items.length) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(AppSpacing.s16),
                child: CircularProgressIndicator(),
              ),
            );
          }

          final item = state.items[index];
          return DeliveryBubble(
            item: item,
            onTap: () => context.push('/notifications/${item.notificationId}'),
          );
        },
      ),
    );
  }

  Widget _buildError(String error) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, size: 64, color: AppColors.error),
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

class _EmptyFeedState extends StatelessWidget {
  const _EmptyFeedState();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.send_outlined,
            size: 64,
            color: Colors.grey,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            '전달된 알림이 없습니다',
            style: AppTextStyles.headlineSmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '채널을 추가하면 알림이 여기에 표시됩니다',
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.s24),
          ElevatedButton(
            onPressed: () => context.push('/channels'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
            ),
            child: const Text('채널 관리로 이동'),
          ),
        ],
      ),
    );
  }
}
