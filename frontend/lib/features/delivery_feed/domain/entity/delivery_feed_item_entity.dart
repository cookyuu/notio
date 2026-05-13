import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

class DeliveryFeedItemEntity {
  final int deliveryLogId;
  final int notificationId;
  final String notificationTitle;
  final int channelId;
  final ChannelTypeEnum channelType;
  final String channelDisplayName;
  final String deliveredContent;
  final DateTime deliveredAt;
  final String status;
  final String? externalMessageId;

  const DeliveryFeedItemEntity({
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
}
