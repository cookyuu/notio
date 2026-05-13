import 'package:notio_app/features/delivery_feed/data/datasource/delivery_feed_remote_datasource.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';
import 'package:notio_app/features/delivery_feed/domain/repository/delivery_feed_repository.dart';

class DeliveryFeedRepositoryImpl implements DeliveryFeedRepository {
  final DeliveryFeedRemoteDataSource _remoteDataSource;

  DeliveryFeedRepositoryImpl({required DeliveryFeedRemoteDataSource remoteDataSource})
      : _remoteDataSource = remoteDataSource;

  @override
  Future<List<DeliveryFeedItemEntity>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  }) async {
    final models = await _remoteDataSource.fetchDeliveryFeed(
      page: page,
      size: size,
      channelType: channelType,
    );
    return models.map((m) => m.toEntity()).toList();
  }
}
