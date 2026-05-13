import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/channels/domain/entity/routing_rule_entity.dart';
import 'package:notio_app/features/channels/presentation/providers/channel_providers.dart';
import 'package:notio_app/features/channels/presentation/providers/routing_rule_providers.dart';

class RoutingRulesScreen extends ConsumerStatefulWidget {
  const RoutingRulesScreen({super.key});

  @override
  ConsumerState<RoutingRulesScreen> createState() => _RoutingRulesScreenState();
}

class _RoutingRulesScreenState extends ConsumerState<RoutingRulesScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(routingRuleNotifierProvider.notifier).load();
      ref.read(channelNotifierProvider.notifier).load();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(routingRuleNotifierProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('라우팅 규칙'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
      ),
      body: _buildBody(state),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showRuleForm(context),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildBody(RoutingRuleState state) {
    if (state.isLoading && state.rules.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.rules.isEmpty) {
      return _buildEmptyState();
    }

    return ReorderableListView.builder(
      padding: const EdgeInsets.all(AppSpacing.s16),
      itemCount: state.rules.length,
      onReorder: (oldIndex, newIndex) {
        if (newIndex > oldIndex) newIndex--;
        final rules = [...state.rules];
        final rule = rules.removeAt(oldIndex);
        rules.insert(newIndex, rule);
        ref
            .read(routingRuleNotifierProvider.notifier)
            .reorder(rules.map((r) => r.id).toList());
      },
      itemBuilder: (context, index) {
        final rule = state.rules[index];
        return _RuleCard(
          key: ValueKey(rule.id),
          rule: rule,
          onEdit: () => _showRuleForm(context, rule: rule),
          onDelete: () => _confirmDelete(context, rule),
        );
      },
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.rule_outlined,
            size: 64,
            color: AppColors.textTertiary,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            '등록된 규칙이 없습니다',
            style: AppTextStyles.titleMedium
                .copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '+ 버튼을 눌러 라우팅 규칙을 추가하세요',
            style:
                AppTextStyles.bodySmall.copyWith(color: AppColors.textTertiary),
          ),
        ],
      ),
    );
  }

  void _showRuleForm(BuildContext context, {RoutingRuleEntity? rule}) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => _RoutingRuleForm(existingRule: rule),
    );
  }

  void _confirmDelete(BuildContext context, RoutingRuleEntity rule) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('규칙 삭제'),
        content: Text('"${rule.ruleName}" 규칙을 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              ref
                  .read(routingRuleNotifierProvider.notifier)
                  .deleteRule(rule.id);
            },
            child: const Text('삭제',
                style: TextStyle(color: AppColors.error)),
          ),
        ],
      ),
    );
  }
}

class _RuleCard extends StatelessWidget {
  const _RuleCard({
    required this.rule,
    required this.onEdit,
    required this.onDelete,
    super.key,
  });

  final RoutingRuleEntity rule;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.s12),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.s12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Padding(
                padding: EdgeInsets.only(top: AppSpacing.s4),
                child: Icon(Icons.drag_handle, color: AppColors.textTertiary),
              ),
              const SizedBox(width: AppSpacing.s8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(rule.ruleName,
                              style: AppTextStyles.bodyLarge),
                        ),
                        _DeliveryModeBadge(
                          mode: rule.deliveryMode,
                          intervalMin: rule.digestIntervalMin,
                        ),
                      ],
                    ),
                    if (rule.conditions.sources.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.s8),
                      _ChipRow(
                        label: '소스',
                        items: rule.conditions.sources,
                        color: AppColors.info,
                      ),
                    ],
                    if (rule.conditions.priorities.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.s4),
                      _ChipRow(
                        label: '우선순위',
                        items: rule.conditions.priorities,
                        color: AppColors.warning,
                      ),
                    ],
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        TextButton(
                          onPressed: onEdit,
                          child: const Text('편집'),
                        ),
                        TextButton(
                          onPressed: onDelete,
                          style: TextButton.styleFrom(
                            foregroundColor: AppColors.error,
                          ),
                          child: const Text('삭제'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ChipRow extends StatelessWidget {
  const _ChipRow({
    required this.label,
    required this.items,
    required this.color,
  });

  final String label;
  final List<String> items;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 4,
      runSpacing: 4,
      children: [
        Text(
          '$label: ',
          style:
              AppTextStyles.labelSmall.copyWith(color: AppColors.textTertiary),
        ),
        ...items.map(
          (item) => Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: color.withAlpha(30),
              border: Border.all(color: color.withAlpha(80)),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              item,
              style: TextStyle(fontSize: 11, color: color),
            ),
          ),
        ),
      ],
    );
  }
}

class _DeliveryModeBadge extends StatelessWidget {
  const _DeliveryModeBadge({required this.mode, this.intervalMin});

  final DeliveryModeEnum mode;
  final int? intervalMin;

  @override
  Widget build(BuildContext context) {
    final label = mode == DeliveryModeEnum.digest && intervalMin != null
        ? '묶음 ${intervalMin! >= 60 ? '1시간' : '$intervalMin분'}'
        : mode.displayName;

    final color = mode == DeliveryModeEnum.digest
        ? AppColors.primaryLight
        : AppColors.success;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withAlpha(30),
        border: Border.all(color: color.withAlpha(80)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          color: color,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _RoutingRuleForm extends ConsumerStatefulWidget {
  const _RoutingRuleForm({this.existingRule});

  final RoutingRuleEntity? existingRule;

  @override
  ConsumerState<_RoutingRuleForm> createState() => _RoutingRuleFormState();
}

class _RoutingRuleFormState extends ConsumerState<_RoutingRuleForm> {
  static const _allSources = ['CLAUDE', 'CODEX', 'GITHUB', 'SLACK', 'GMAIL'];
  static const _allPriorities = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];

  late final TextEditingController _nameController;
  late Set<String> _selectedSources;
  late Set<String> _selectedPriorities;
  late Set<int> _selectedChannelIds;
  late bool _stopOnMatch;
  late DeliveryModeEnum _deliveryMode;
  late int _digestInterval;

  @override
  void initState() {
    super.initState();
    final rule = widget.existingRule;
    _nameController = TextEditingController(text: rule?.ruleName ?? '');
    _selectedSources = Set.from(rule?.conditions.sources ?? []);
    _selectedPriorities = Set.from(rule?.conditions.priorities ?? []);
    _selectedChannelIds = Set.from(rule?.channelIds ?? []);
    _stopOnMatch = rule?.stopOnMatch ?? false;
    _deliveryMode = rule?.deliveryMode ?? DeliveryModeEnum.immediate;
    _digestInterval = rule?.digestIntervalMin ?? 10;
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final channels = ref.watch(channelNotifierProvider).channels;

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.only(
            left: AppSpacing.s24,
            right: AppSpacing.s24,
            top: AppSpacing.s24,
            bottom:
                MediaQuery.viewInsetsOf(context).bottom + AppSpacing.s24,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                widget.existingRule == null ? '규칙 추가' : '규칙 편집',
                style: AppTextStyles.headlineMedium,
              ),
              const SizedBox(height: AppSpacing.s24),

              // Rule name
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: '규칙 이름'),
              ),
              const SizedBox(height: AppSpacing.s16),

              // Sources
              _buildMultiSelect(
                label: '소스 (빈 값 = 전체)',
                options: _allSources,
                selected: _selectedSources,
                onToggle: (val) => setState(() {
                  _selectedSources.contains(val)
                      ? _selectedSources.remove(val)
                      : _selectedSources.add(val);
                }),
              ),
              const SizedBox(height: AppSpacing.s16),

              // Priorities
              _buildMultiSelect(
                label: '우선순위 (빈 값 = 전체)',
                options: _allPriorities,
                selected: _selectedPriorities,
                onToggle: (val) => setState(() {
                  _selectedPriorities.contains(val)
                      ? _selectedPriorities.remove(val)
                      : _selectedPriorities.add(val);
                }),
              ),
              const SizedBox(height: AppSpacing.s16),

              // Target channels
              if (channels.isNotEmpty) ...[
                Text(
                  '대상 채널',
                  style: AppTextStyles.labelMedium
                      .copyWith(color: AppColors.textSecondary),
                ),
                const SizedBox(height: AppSpacing.s8),
                Wrap(
                  spacing: AppSpacing.s8,
                  runSpacing: AppSpacing.s4,
                  children: channels.map((ch) {
                    final selected = _selectedChannelIds.contains(ch.id);
                    return FilterChip(
                      label: Text(ch.displayName),
                      selected: selected,
                      onSelected: (_) => setState(() {
                        selected
                            ? _selectedChannelIds.remove(ch.id)
                            : _selectedChannelIds.add(ch.id);
                      }),
                    );
                  }).toList(),
                ),
                const SizedBox(height: AppSpacing.s16),
              ],

              // Stop on match
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('이 규칙 매칭 후 중단'),
                subtitle: Text(
                  '다음 규칙 평가를 건너뜁니다',
                  style: AppTextStyles.bodySmall
                      .copyWith(color: AppColors.textTertiary),
                ),
                value: _stopOnMatch,
                onChanged: (val) => setState(() => _stopOnMatch = val),
                activeThumbColor: AppColors.primary,
              ),
              const SizedBox(height: AppSpacing.s16),

              // Delivery mode
              Text(
                '전달 방식',
                style: AppTextStyles.labelMedium
                    .copyWith(color: AppColors.textSecondary),
              ),
              const SizedBox(height: AppSpacing.s8),
              SegmentedButton<DeliveryModeEnum>(
                segments: const [
                  ButtonSegment(
                    value: DeliveryModeEnum.immediate,
                    label: Text('즉시 전송'),
                  ),
                  ButtonSegment(
                    value: DeliveryModeEnum.digest,
                    label: Text('묶음 전송'),
                  ),
                ],
                selected: {_deliveryMode},
                onSelectionChanged: (modes) =>
                    setState(() => _deliveryMode = modes.first),
              ),

              // Digest interval (DIGEST 선택 시만 표시)
              if (_deliveryMode == DeliveryModeEnum.digest) ...[
                const SizedBox(height: AppSpacing.s16),
                Text(
                  '묶음 간격',
                  style: AppTextStyles.labelMedium
                      .copyWith(color: AppColors.textSecondary),
                ),
                const SizedBox(height: AppSpacing.s8),
                SegmentedButton<int>(
                  segments: const [
                    ButtonSegment(value: 10, label: Text('10분')),
                    ButtonSegment(value: 20, label: Text('20분')),
                    ButtonSegment(value: 30, label: Text('30분')),
                    ButtonSegment(value: 60, label: Text('1시간')),
                  ],
                  selected: {_digestInterval},
                  onSelectionChanged: (vals) =>
                      setState(() => _digestInterval = vals.first),
                ),
              ],

              const SizedBox(height: AppSpacing.s24),

              // Save button
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _submit,
                  child: const Text('저장'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMultiSelect({
    required String label,
    required List<String> options,
    required Set<String> selected,
    required void Function(String) onToggle,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTextStyles.labelMedium
              .copyWith(color: AppColors.textSecondary),
        ),
        const SizedBox(height: AppSpacing.s8),
        Wrap(
          spacing: AppSpacing.s8,
          runSpacing: AppSpacing.s4,
          children: options
              .map((opt) => FilterChip(
                    label: Text(opt),
                    selected: selected.contains(opt),
                    onSelected: (_) => onToggle(opt),
                  ))
              .toList(),
        ),
      ],
    );
  }

  Future<void> _submit() async {
    if (_nameController.text.trim().isEmpty) return;

    final notifier = ref.read(routingRuleNotifierProvider.notifier);
    final rule = widget.existingRule;

    final bool success;
    if (rule == null) {
      success = await notifier.createRule(
        ruleName: _nameController.text.trim(),
        sources: _selectedSources.toList(),
        priorities: _selectedPriorities.toList(),
        channelIds: _selectedChannelIds.toList(),
        stopOnMatch: _stopOnMatch,
        deliveryMode: _deliveryMode,
        digestIntervalMin: _deliveryMode == DeliveryModeEnum.digest
            ? _digestInterval
            : null,
      );
    } else {
      success = await notifier.updateRule(
        id: rule.id,
        ruleName: _nameController.text.trim(),
        sources: _selectedSources.toList(),
        priorities: _selectedPriorities.toList(),
        channelIds: _selectedChannelIds.toList(),
        stopOnMatch: _stopOnMatch,
        deliveryMode: _deliveryMode,
        digestIntervalMin: _deliveryMode == DeliveryModeEnum.digest
            ? _digestInterval
            : null,
      );
    }

    if (success && mounted) {
      Navigator.pop(context);
    }
  }
}
