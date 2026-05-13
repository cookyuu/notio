import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:notio_app/features/channels/domain/entity/routing_rule_entity.dart';
import 'channel_providers.dart';

part 'routing_rule_providers.g.dart';

class RoutingRuleState {
  final List<RoutingRuleEntity> rules;
  final bool isLoading;
  final bool isActing;
  final String? error;

  const RoutingRuleState({
    this.rules = const [],
    this.isLoading = false,
    this.isActing = false,
    this.error,
  });

  RoutingRuleState copyWith({
    List<RoutingRuleEntity>? rules,
    bool? isLoading,
    bool? isActing,
    String? error,
    bool clearError = false,
  }) {
    return RoutingRuleState(
      rules: rules ?? this.rules,
      isLoading: isLoading ?? this.isLoading,
      isActing: isActing ?? this.isActing,
      error: clearError ? null : (error ?? this.error),
    );
  }
}

@riverpod
class RoutingRuleNotifier extends _$RoutingRuleNotifier {
  @override
  RoutingRuleState build() => const RoutingRuleState();

  Future<void> load() async {
    if (state.isLoading) return;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final rules =
          await ref.read(channelRepositoryProvider).fetchRoutingRules();
      final sorted = [...rules]
        ..sort((a, b) => a.priorityOrder.compareTo(b.priorityOrder));
      state = state.copyWith(rules: sorted, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  Future<bool> createRule({
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  }) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      await ref.read(channelRepositoryProvider).createRoutingRule(
            ruleName: ruleName,
            sources: sources,
            priorities: priorities,
            channelIds: channelIds,
            stopOnMatch: stopOnMatch,
            isEnabled: true,
            deliveryMode: deliveryMode,
            digestIntervalMin: digestIntervalMin,
          );
      await load();
      return true;
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
      return false;
    }
  }

  Future<bool> updateRule({
    required int id,
    required String ruleName,
    required List<String> sources,
    required List<String> priorities,
    required List<int> channelIds,
    required bool stopOnMatch,
    required DeliveryModeEnum deliveryMode,
    int? digestIntervalMin,
  }) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      await ref.read(channelRepositoryProvider).updateRoutingRule(
            id: id,
            ruleName: ruleName,
            sources: sources,
            priorities: priorities,
            channelIds: channelIds,
            stopOnMatch: stopOnMatch,
            isEnabled: true,
            deliveryMode: deliveryMode,
            digestIntervalMin: digestIntervalMin,
          );
      await load();
      return true;
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
      return false;
    }
  }

  Future<void> deleteRule(int id) async {
    state = state.copyWith(isActing: true, clearError: true);
    try {
      await ref.read(channelRepositoryProvider).deleteRoutingRule(id);
      await load();
    } catch (e) {
      state = state.copyWith(isActing: false, error: e.toString());
    }
  }

  Future<void> reorder(List<int> orderedIds) async {
    try {
      await ref
          .read(channelRepositoryProvider)
          .reorderRoutingRules(orderedIds);
      await load();
    } catch (e) {
      state = state.copyWith(error: e.toString());
    }
  }
}
