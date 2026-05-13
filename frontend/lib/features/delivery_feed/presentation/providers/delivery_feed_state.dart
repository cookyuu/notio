import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';

class DeliveryFeedState {
  final List<DeliveryFeedItemEntity> items;
  final bool isLoading;
  final bool isLoadingMore;
  final bool hasMore;
  final int page;
  final ChannelTypeEnum? filter;
  final String? error;

  const DeliveryFeedState({
    this.items = const [],
    this.isLoading = false,
    this.isLoadingMore = false,
    this.hasMore = true,
    this.page = 0,
    this.filter,
    this.error,
  });

  DeliveryFeedState copyWith({
    List<DeliveryFeedItemEntity>? items,
    bool? isLoading,
    bool? isLoadingMore,
    bool? hasMore,
    int? page,
    ChannelTypeEnum? filter,
    bool clearFilter = false,
    String? error,
    bool clearError = false,
  }) {
    return DeliveryFeedState(
      items: items ?? this.items,
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      hasMore: hasMore ?? this.hasMore,
      page: page ?? this.page,
      filter: clearFilter ? null : (filter ?? this.filter),
      error: clearError ? null : (error ?? this.error),
    );
  }
}
