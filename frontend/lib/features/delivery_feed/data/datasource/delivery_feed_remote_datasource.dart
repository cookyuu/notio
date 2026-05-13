import 'package:dio/dio.dart';
import 'package:notio_app/features/delivery_feed/data/model/delivery_feed_item_model.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

abstract interface class DeliveryFeedRemoteDataSource {
  Future<List<DeliveryFeedItemModel>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  });
}

class DeliveryFeedRemoteDataSourceImpl implements DeliveryFeedRemoteDataSource {
  final Dio _dio;

  DeliveryFeedRemoteDataSourceImpl(this._dio);

  @override
  Future<List<DeliveryFeedItemModel>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  }) async {
    try {
      final response = await _dio.get(
        '/api/v1/channels/delivery-feed',
        queryParameters: {
          'page': page,
          'size': size,
          if (channelType != null) 'channelType': channelType.apiValue,
        },
      );

      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }

      final data = response.data['data'];
      final List<dynamic> items = switch (data) {
        List<dynamic>() => data,
        Map<String, dynamic>() =>
          (data['content'] as List<dynamic>?) ?? const [],
        _ => const <dynamic>[],
      };

      return items
          .map((json) =>
              DeliveryFeedItemModel.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }
}
