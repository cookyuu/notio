import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_status.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../providers/connection_providers.dart';
import '../widgets/connection_card.dart';
import 'create_connection_sheet.dart';

/// Connections management screen
class ConnectionsScreen extends ConsumerWidget {
  const ConnectionsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final connectionsAsync = ref.watch(connectionsProvider);
    final providerFilter = ref.watch(connectionProviderFilterProvider);
    final statusFilter = ref.watch(connectionStatusFilterProvider);
    final authTypeFilter = ref.watch(connectionAuthTypeFilterProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('연동 관리'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              _showCreateConnectionSheet(context);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Filters
          _buildFilters(context, ref, providerFilter, statusFilter, authTypeFilter),

          // Connections List
          Expanded(
            child: connectionsAsync.when(
              data: (connections) {
                final filteredConnections = _applyFilters(
                  connections,
                  providerFilter,
                  statusFilter,
                  authTypeFilter,
                );

                if (filteredConnections.isEmpty) {
                  return _buildEmptyState();
                }

                return RefreshIndicator(
                  onRefresh: () async {
                    await ref.read(connectionsProvider.notifier).refresh();
                  },
                  child: ListView.builder(
                    padding: const EdgeInsets.all(AppSpacing.s16),
                    itemCount: filteredConnections.length,
                    itemBuilder: (context, index) {
                      final connection = filteredConnections[index];
                      return ConnectionCard(
                        connection: connection,
                        onTap: () {
                          context.push('/settings/connections/${connection.id}');
                        },
                      );
                    },
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, stack) => _buildErrorState(error.toString()),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilters(
    BuildContext context,
    WidgetRef ref,
    ConnectionProvider? providerFilter,
    ConnectionStatus? statusFilter,
    ConnectionAuthType? authTypeFilter,
  ) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.s16),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(
          bottom: BorderSide(
            color: AppColors.divider,
            width: 0.5,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Filters',
            style: AppTextStyles.labelMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.s8),

          // Status Filter
          Wrap(
            spacing: AppSpacing.s8,
            children: [
              FilterChip(
                label: const Text('All'),
                selected: statusFilter == null,
                onSelected: (selected) {
                  ref.read(connectionStatusFilterProvider.notifier).state = null;
                },
              ),
              FilterChip(
                label: const Text('Active'),
                selected: statusFilter == ConnectionStatus.active,
                onSelected: (selected) {
                  ref.read(connectionStatusFilterProvider.notifier).state =
                      selected ? ConnectionStatus.active : null;
                },
              ),
              FilterChip(
                label: const Text('Needs Action'),
                selected: statusFilter == ConnectionStatus.needsAction,
                onSelected: (selected) {
                  ref.read(connectionStatusFilterProvider.notifier).state =
                      selected ? ConnectionStatus.needsAction : null;
                },
              ),
            ],
          ),

          const SizedBox(height: AppSpacing.s8),

          // Auth Type Filter
          Wrap(
            spacing: AppSpacing.s8,
            children: [
              FilterChip(
                label: const Text('All Types'),
                selected: authTypeFilter == null,
                onSelected: (selected) {
                  ref.read(connectionAuthTypeFilterProvider.notifier).state = null;
                },
              ),
              FilterChip(
                label: const Text('API Key'),
                selected: authTypeFilter == ConnectionAuthType.apiKey,
                onSelected: (selected) {
                  ref.read(connectionAuthTypeFilterProvider.notifier).state =
                      selected ? ConnectionAuthType.apiKey : null;
                },
              ),
              FilterChip(
                label: const Text('OAuth'),
                selected: authTypeFilter == ConnectionAuthType.oauth,
                onSelected: (selected) {
                  ref.read(connectionAuthTypeFilterProvider.notifier).state =
                      selected ? ConnectionAuthType.oauth : null;
                },
              ),
            ],
          ),
        ],
      ),
    );
  }

  List<dynamic> _applyFilters(
    List<dynamic> connections,
    ConnectionProvider? providerFilter,
    ConnectionStatus? statusFilter,
    ConnectionAuthType? authTypeFilter,
  ) {
    var filtered = connections;

    if (providerFilter != null) {
      filtered = filtered.where((c) => c.provider == providerFilter).toList();
    }

    if (statusFilter != null) {
      filtered = filtered.where((c) => c.status == statusFilter).toList();
    }

    if (authTypeFilter != null) {
      filtered = filtered.where((c) => c.authType == authTypeFilter).toList();
    }

    return filtered;
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.hub_outlined,
            size: 64,
            color: AppColors.textTertiary,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            'No connections yet',
            style: AppTextStyles.titleMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            'Tap + to add your first connection',
            style: AppTextStyles.bodySmall.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState(String error) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.error_outline,
            size: 64,
            color: AppColors.error,
          ),
          const SizedBox(height: AppSpacing.s16),
          Text(
            'Error loading connections',
            style: AppTextStyles.titleMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            error,
            style: AppTextStyles.bodySmall.copyWith(
              color: AppColors.textTertiary,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  void _showCreateConnectionSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => const CreateConnectionSheet(),
    );
  }
}
