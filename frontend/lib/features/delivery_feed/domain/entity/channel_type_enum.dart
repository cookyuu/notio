enum ChannelTypeEnum {
  slack,
  telegram,
  discord,
}

extension ChannelTypeEnumExtension on ChannelTypeEnum {
  String get apiValue {
    switch (this) {
      case ChannelTypeEnum.slack:
        return 'SLACK';
      case ChannelTypeEnum.telegram:
        return 'TELEGRAM';
      case ChannelTypeEnum.discord:
        return 'DISCORD';
    }
  }

  String get displayName {
    switch (this) {
      case ChannelTypeEnum.slack:
        return 'Slack';
      case ChannelTypeEnum.telegram:
        return 'Telegram';
      case ChannelTypeEnum.discord:
        return 'Discord';
    }
  }

  static ChannelTypeEnum fromApiValue(String value) {
    switch (value.toUpperCase()) {
      case 'SLACK':
        return ChannelTypeEnum.slack;
      case 'TELEGRAM':
        return ChannelTypeEnum.telegram;
      case 'DISCORD':
        return ChannelTypeEnum.discord;
      default:
        return ChannelTypeEnum.slack;
    }
  }
}
