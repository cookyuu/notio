import '../entity/notification_channel_entity.dart';
import '../entity/routing_rule_entity.dart';

abstract class ChannelRepository {
  Future<List<NotificationChannelEntity>> fetchChannels();

  Future<NotificationChannelEntity> createChannel({
    required String displayName,
    required String channelType,
    required String credentialPlaintext,
    String? targetIdentifier,
  });

  Future<void> deleteChannel(int id);

  Future<void> pauseChannel(int id);

  Future<void> resumeChannel(int id);

  Future<void> testChannel(int id);

  Future<List<RoutingRuleEntity>> fetchRoutingRules();

  Future<RoutingRuleEntity> createRoutingRule({
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required bool isEnabled,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  });

  Future<RoutingRuleEntity> updateRoutingRule({
    required int id,
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required bool isEnabled,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  });

  Future<void> deleteRoutingRule(int id);

  Future<void> reorderRoutingRules(List<int> orderedIds);
}
