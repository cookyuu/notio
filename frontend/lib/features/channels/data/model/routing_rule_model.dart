import 'package:json_annotation/json_annotation.dart';
import 'package:notio_app/features/channels/domain/entity/routing_rule_entity.dart';

part 'routing_rule_model.g.dart';

@JsonSerializable()
class RoutingConditionModel {
  final List<String> sources;
  final List<String> priorities;

  const RoutingConditionModel({
    this.sources = const [],
    this.priorities = const [],
  });

  factory RoutingConditionModel.fromJson(Map<String, dynamic> json) =>
      _$RoutingConditionModelFromJson(json);

  Map<String, dynamic> toJson() => _$RoutingConditionModelToJson(this);
}

@JsonSerializable()
class RoutingRuleModel {
  final int id;
  @JsonKey(name: 'rule_name')
  final String ruleName;
  @JsonKey(name: 'priority_order')
  final int priorityOrder;
  final RoutingConditionModel conditions;
  @JsonKey(name: 'channel_ids')
  final List<int> channelIds;
  @JsonKey(name: 'stop_on_match')
  final bool stopOnMatch;
  @JsonKey(name: 'is_enabled')
  final bool isEnabled;
  @JsonKey(name: 'delivery_mode')
  final String deliveryMode;
  @JsonKey(name: 'digest_interval_min')
  final int? digestIntervalMin;

  const RoutingRuleModel({
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

  factory RoutingRuleModel.fromJson(Map<String, dynamic> json) =>
      _$RoutingRuleModelFromJson(json);

  Map<String, dynamic> toJson() => _$RoutingRuleModelToJson(this);

  RoutingRuleEntity toEntity() {
    return RoutingRuleEntity(
      id: id,
      ruleName: ruleName,
      priorityOrder: priorityOrder,
      conditions: RoutingConditionEntity(
        sources: conditions.sources,
        priorities: conditions.priorities,
      ),
      channelIds: channelIds,
      stopOnMatch: stopOnMatch,
      isEnabled: isEnabled,
      deliveryMode: DeliveryModeEnumExtension.fromApiValue(deliveryMode),
      digestIntervalMin: digestIntervalMin,
    );
  }
}
