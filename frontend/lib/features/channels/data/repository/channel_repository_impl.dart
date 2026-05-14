import 'package:notio_app/features/channels/data/datasource/channel_remote_datasource.dart';
import 'package:notio_app/features/channels/domain/entity/notification_channel_entity.dart';
import 'package:notio_app/features/channels/domain/entity/routing_rule_entity.dart';
import 'package:notio_app/features/channels/domain/repository/channel_repository.dart';

class ChannelRepositoryImpl implements ChannelRepository {
  final ChannelRemoteDataSource _remoteDataSource;

  ChannelRepositoryImpl({required ChannelRemoteDataSource remoteDataSource})
      : _remoteDataSource = remoteDataSource;

  @override
  Future<List<NotificationChannelEntity>> fetchChannels() async {
    final models = await _remoteDataSource.fetchChannels();
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<NotificationChannelEntity> createChannel({
    required String displayName,
    required String channelType,
    required String credentialPlaintext,
    String? targetIdentifier,
  }) async {
    final model = await _remoteDataSource.createChannel({
      'display_name': displayName,
      'channel_type': channelType,
      'credential_plaintext': credentialPlaintext,
      if (targetIdentifier != null) 'target_identifier': targetIdentifier,
    });
    return model.toEntity();
  }

  @override
  Future<NotificationChannelEntity> updateChannel({
    required int id,
    String? displayName,
    String? credentialPlaintext,
    String? targetIdentifier,
  }) async {
    final model = await _remoteDataSource.updateChannel(id, {
      if (displayName != null) 'display_name': displayName,
      if (credentialPlaintext != null)
        'credential_plaintext': credentialPlaintext,
      if (targetIdentifier != null) 'target_identifier': targetIdentifier,
    });
    return model.toEntity();
  }

  @override
  Future<void> deleteChannel(int id) async {
    await _remoteDataSource.deleteChannel(id);
  }

  @override
  Future<void> pauseChannel(int id) async {
    await _remoteDataSource.pauseChannel(id);
  }

  @override
  Future<void> resumeChannel(int id) async {
    await _remoteDataSource.resumeChannel(id);
  }

  @override
  Future<void> testChannel(int id) async {
    await _remoteDataSource.testChannel(id);
  }

  @override
  Future<List<RoutingRuleEntity>> fetchRoutingRules() async {
    final models = await _remoteDataSource.fetchRoutingRules();
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<RoutingRuleEntity> createRoutingRule({
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required bool isEnabled,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  }) async {
    final model = await _remoteDataSource.createRoutingRule({
      'rule_name': ruleName,
      'conditions': {'sources': sources, 'priorities': priorities},
      'channel_ids': channelIds,
      'stop_on_match': stopOnMatch,
      'is_enabled': isEnabled,
      'delivery_mode': deliveryMode.apiValue,
      if (digestIntervalMin != null) 'digest_interval_min': digestIntervalMin,
    });
    return model.toEntity();
  }

  @override
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
  }) async {
    final model = await _remoteDataSource.updateRoutingRule(id, {
      'rule_name': ruleName,
      'conditions': {'sources': sources, 'priorities': priorities},
      'channel_ids': channelIds,
      'stop_on_match': stopOnMatch,
      'is_enabled': isEnabled,
      'delivery_mode': deliveryMode.apiValue,
      if (digestIntervalMin != null) 'digest_interval_min': digestIntervalMin,
    });
    return model.toEntity();
  }

  @override
  Future<void> deleteRoutingRule(int id) async {
    await _remoteDataSource.deleteRoutingRule(id);
  }

  @override
  Future<void> reorderRoutingRules(List<int> orderedIds) async {
    await _remoteDataSource.reorderRoutingRules(orderedIds);
  }
}
