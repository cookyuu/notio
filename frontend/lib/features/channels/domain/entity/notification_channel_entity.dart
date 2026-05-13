import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

enum ChannelStatusEnum { active, paused, error }

extension ChannelStatusEnumExtension on ChannelStatusEnum {
  String get apiValue {
    switch (this) {
      case ChannelStatusEnum.active:
        return 'ACTIVE';
      case ChannelStatusEnum.paused:
        return 'PAUSED';
      case ChannelStatusEnum.error:
        return 'ERROR';
    }
  }

  static ChannelStatusEnum fromApiValue(String value) {
    return switch (value.toUpperCase()) {
      'ACTIVE' => ChannelStatusEnum.active,
      'PAUSED' => ChannelStatusEnum.paused,
      'ERROR' => ChannelStatusEnum.error,
      _ => ChannelStatusEnum.active,
    };
  }
}

class NotificationChannelEntity {
  final int id;
  final ChannelTypeEnum channelType;
  final String displayName;
  final String? keyPreview;
  final ChannelStatusEnum status;
  final int errorCount;
  final String? lastError;
  final DateTime? lastDeliveredAt;
  final DateTime createdAt;

  const NotificationChannelEntity({
    required this.id,
    required this.channelType,
    required this.displayName,
    this.keyPreview,
    required this.status,
    required this.errorCount,
    this.lastError,
    this.lastDeliveredAt,
    required this.createdAt,
  });
}
