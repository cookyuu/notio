import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';

abstract interface class DeliveryFeedRepository {
  Future<List<DeliveryFeedItemEntity>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  });
}
