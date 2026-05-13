import 'package:json_annotation/json_annotation.dart';
import 'package:notio_app/features/channels/domain/entity/notification_channel_entity.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

part 'notification_channel_model.g.dart';

@JsonSerializable()
class NotificationChannelModel {
  final int id;
  @JsonKey(name: 'channel_type')
  final String channelType;
  @JsonKey(name: 'display_name')
  final String displayName;
  @JsonKey(name: 'key_preview')
  final String? keyPreview;
  final String status;
  @JsonKey(name: 'error_count', defaultValue: 0)
  final int errorCount;
  @JsonKey(name: 'last_error')
  final String? lastError;
  @JsonKey(name: 'last_delivered_at')
  final String? lastDeliveredAt;
  @JsonKey(name: 'created_at')
  final String createdAt;

  const NotificationChannelModel({
    required this.id,
    required this.channelType,
    required this.displayName,
    required this.status,
    required this.errorCount,
    required this.createdAt,
    this.keyPreview,
    this.lastError,
    this.lastDeliveredAt,
  });

  factory NotificationChannelModel.fromJson(Map<String, dynamic> json) =>
      _$NotificationChannelModelFromJson(json);

  Map<String, dynamic> toJson() => _$NotificationChannelModelToJson(this);

  NotificationChannelEntity toEntity() {
    return NotificationChannelEntity(
      id: id,
      channelType: ChannelTypeEnumExtension.fromApiValue(channelType),
      displayName: displayName,
      keyPreview: keyPreview,
      status: ChannelStatusEnumExtension.fromApiValue(status),
      errorCount: errorCount,
      lastError: lastError,
      lastDeliveredAt:
          lastDeliveredAt != null ? DateTime.parse(lastDeliveredAt!) : null,
      createdAt: DateTime.parse(createdAt),
    );
  }
}
