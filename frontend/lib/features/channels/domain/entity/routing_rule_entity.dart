enum DeliveryModeEnum { immediate, digest }

extension DeliveryModeEnumExtension on DeliveryModeEnum {
  String get apiValue {
    switch (this) {
      case DeliveryModeEnum.immediate:
        return 'IMMEDIATE';
      case DeliveryModeEnum.digest:
        return 'DIGEST';
    }
  }

  String get displayName {
    switch (this) {
      case DeliveryModeEnum.immediate:
        return '즉시 전송';
      case DeliveryModeEnum.digest:
        return '묶음 전송';
    }
  }

  static DeliveryModeEnum fromApiValue(String value) {
    return switch (value.toUpperCase()) {
      'DIGEST' => DeliveryModeEnum.digest,
      _ => DeliveryModeEnum.immediate,
    };
  }
}

class RoutingConditionEntity {
  final List<String> sources;
  final List<String> priorities;

  const RoutingConditionEntity({
    required this.sources,
    required this.priorities,
  });
}

class RoutingRuleEntity {
  final int id;
  final String ruleName;
  final int priorityOrder;
  final RoutingConditionEntity conditions;
  final List<int> channelIds;
  final bool stopOnMatch;
  final bool isEnabled;
  final DeliveryModeEnum deliveryMode;
  final int? digestIntervalMin;

  const RoutingRuleEntity({
    required this.id,
    required this.ruleName,
    required this.priorityOrder,
    required this.conditions,
    required this.channelIds,
    required this.stopOnMatch,
    required this.isEnabled,
    required this.deliveryMode,
    this.digestIntervalMin,
  });
}
