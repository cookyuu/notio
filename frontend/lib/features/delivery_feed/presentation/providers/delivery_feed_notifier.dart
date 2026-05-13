import 'package:notio_app/features/delivery_feed/data/datasource/delivery_feed_remote_datasource.dart';
import 'package:notio_app/features/delivery_feed/data/repository/delivery_feed_repository_impl.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/repository/delivery_feed_repository.dart';
import 'package:notio_app/features/delivery_feed/presentation/providers/delivery_feed_state.dart';
import 'package:notio_app/features/notification/presentation/providers/notification_providers.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'delivery_feed_notifier.g.dart';

@riverpod
DeliveryFeedRemoteDataSource deliveryFeedRemoteDataSource(
    DeliveryFeedRemoteDataSourceRef ref) {
  final dio = ref.watch(dioProvider);
  return DeliveryFeedRemoteDataSourceImpl(dio);
}

@riverpod
DeliveryFeedRepository deliveryFeedRepository(DeliveryFeedRepositoryRef ref) {
  return DeliveryFeedRepositoryImpl(
    remoteDataSource: ref.watch(deliveryFeedRemoteDataSourceProvider),
  );
}

@riverpod
class DeliveryFeedNotifier extends _$DeliveryFeedNotifier {
  static const _pageSize = 20;

  @override
  DeliveryFeedState build() => const DeliveryFeedState();

  Future<void> load() async {
    if (state.isLoading) return;

    state = state.copyWith(isLoading: true, clearError: true);

    try {
      final repository = ref.read(deliveryFeedRepositoryProvider);
      final items = await repository.fetchDeliveryFeed(
        page: 0,
        size: _pageSize,
        channelType: state.filter,
      );

      state = state.copyWith(
        items: items,
        isLoading: false,
        page: 0,
        hasMore: items.length >= _pageSize,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  Future<void> loadMore() async {
    if (!state.hasMore || state.isLoading || state.isLoadingMore) return;

    state = state.copyWith(isLoadingMore: true);

    try {
      final repository = ref.read(deliveryFeedRepositoryProvider);
      final nextPage = state.page + 1;
      final items = await repository.fetchDeliveryFeed(
        page: nextPage,
        size: _pageSize,
        channelType: state.filter,
      );

      state = state.copyWith(
        items: [...state.items, ...items],
        isLoadingMore: false,
        page: nextPage,
        hasMore: items.length >= _pageSize,
      );
    } catch (e) {
      state = state.copyWith(
        isLoadingMore: false,
        error: e.toString(),
      );
    }
  }

  Future<void> setFilter(ChannelTypeEnum? channelType) async {
    state = state.copyWith(
      items: [],
      page: 0,
      hasMore: true,
      filter: channelType,
      clearFilter: channelType == null,
      clearError: true,
    );
    await load();
  }
}
