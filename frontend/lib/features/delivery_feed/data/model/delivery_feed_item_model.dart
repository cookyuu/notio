import 'package:json_annotation/json_annotation.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';

part 'delivery_feed_item_model.g.dart';

@JsonSerializable(fieldRename: FieldRename.snake)
class DeliveryFeedItemModel {
  final int deliveryLogId;
  final int notificationId;
  final String notificationTitle;
  final int channelId;
  final String channelType;
  final String channelDisplayName;
  final String deliveredContent;
  final String deliveredAt;
  final String status;
  final String? externalMessageId;

  const DeliveryFeedItemModel({
    required this.deliveryLogId,
    required this.notificationId,
    required this.notificationTitle,
    required this.channelId,
    required this.channelType,
    required this.channelDisplayName,
    required this.deliveredContent,
    required this.deliveredAt,
    required this.status,
    this.externalMessageId,
  });

  factory DeliveryFeedItemModel.fromJson(Map<String, dynamic> json) =>
      _$DeliveryFeedItemModelFromJson(json);

  Map<String, dynamic> toJson() => _$DeliveryFeedItemModelToJson(this);

  DeliveryFeedItemEntity toEntity() {
    return DeliveryFeedItemEntity(
      deliveryLogId: deliveryLogId,
      notificationId: notificationId,
      notificationTitle: notificationTitle,
      channelId: channelId,
      channelType: ChannelTypeEnumExtension.fromApiValue(channelType),
      channelDisplayName: channelDisplayName,
      deliveredContent: deliveredContent,
      deliveredAt: DateTime.parse(deliveredAt),
      status: status,
      externalMessageId: externalMessageId,
    );
  }
}
