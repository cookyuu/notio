import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../providers/connection_providers.dart';
import '../widgets/connection_provider_icon.dart';
import 'one_time_api_key_dialog.dart';

/// Bottom sheet for creating a new connection
class CreateConnectionSheet extends ConsumerStatefulWidget {
  const CreateConnectionSheet({super.key});

  @override
  ConsumerState<CreateConnectionSheet> createState() => _CreateConnectionSheetState();
}

class _CreateConnectionSheetState extends ConsumerState<CreateConnectionSheet> {
  ConnectionProvider? _selectedProvider;
  ConnectionAuthType? _selectedAuthType;
  final _displayNameController = TextEditingController();

  @override
  void dispose() {
    _displayNameController.dispose();
    super.dispose();
  }

  Map<ConnectionProvider, List<ConnectionAuthType>> _getSupportedAuthTypes() {
    return {
      ConnectionProvider.claude: [ConnectionAuthType.apiKey],
      ConnectionProvider.slack: [ConnectionAuthType.oauth],
      ConnectionProvider.gmail: [ConnectionAuthType.oauth],
      ConnectionProvider.github: [ConnectionAuthType.oauth],
      ConnectionProvider.discord: [ConnectionAuthType.oauth],
      ConnectionProvider.jira: [ConnectionAuthType.oauth, ConnectionAuthType.apiKey],
      ConnectionProvider.linear: [ConnectionAuthType.oauth, ConnectionAuthType.apiKey],
      ConnectionProvider.teams: [ConnectionAuthType.oauth],
    };
  }

  @override
  Widget build(BuildContext context) {
    final actionState = ref.watch(connectionActionsProvider);
    final oneTimeKeyState = ref.watch(oneTimeApiKeyProvider);

    // Show API key dialog if available
    if (oneTimeKeyState.isVisible && oneTimeKeyState.apiKey != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        // Save the API key before clearing the state
        final apiKey = oneTimeKeyState.apiKey!;

        // Immediately clear the state to prevent duplicate dialogs
        ref.read(oneTimeApiKeyProvider.notifier).discardApiKey();

        // Close create sheet
        Navigator.of(context).pop();

        // Show the dialog
        showDialog(
          context: context,
          barrierDismissible: false,
          builder: (context) => OneTimeApiKeyDialog(apiKey: apiKey),
        );
      });
    }

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.s24)),
      ),
      child: SafeArea(
        child: Padding(
          padding: EdgeInsets.only(
            left: AppSpacing.s24,
            right: AppSpacing.s24,
            top: AppSpacing.s24,
            bottom: MediaQuery.of(context).viewInsets.bottom + AppSpacing.s24,
          ),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
              // Header
              const Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Add Connection',
                    style: AppTextStyles.headlineMedium,
                  ),
                ],
              ),
              Align(
                alignment: Alignment.topRight,
                child: IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ),

              const SizedBox(height: AppSpacing.s24),

              // Provider Selection
              Text(
                'Select Provider',
                style: AppTextStyles.titleSmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: AppSpacing.s12),
              _buildProviderGrid(),

              if (_selectedProvider != null) ...[
                const SizedBox(height: AppSpacing.s24),

                // Auth Type Selection
                Text(
                  'Authentication Type',
                  style: AppTextStyles.titleSmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: AppSpacing.s12),
                _buildAuthTypeSelection(),

                const SizedBox(height: AppSpacing.s24),

                // Display Name Input
                Text(
                  'Display Name',
                  style: AppTextStyles.titleSmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: AppSpacing.s12),
                TextField(
                  controller: _displayNameController,
                  decoration: InputDecoration(
                    hintText: 'e.g., My Claude Account',
                    filled: true,
                    fillColor: AppColors.background,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppSpacing.s12),
                      borderSide: const BorderSide(color: AppColors.divider),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppSpacing.s12),
                      borderSide: const BorderSide(color: AppColors.divider),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppSpacing.s12),
                      borderSide: const BorderSide(color: AppColors.primary),
                    ),
                  ),
                ),

                const SizedBox(height: AppSpacing.s24),

                // Create Button
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: actionState.isCreating ? null : _handleCreate,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: AppColors.textPrimary,
                      padding: const EdgeInsets.symmetric(vertical: AppSpacing.s16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(AppSpacing.s12),
                      ),
                    ),
                    child: actionState.isCreating
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor: AlwaysStoppedAnimation<Color>(AppColors.textPrimary),
                            ),
                          )
                        : const Text('Create Connection'),
                  ),
                ),

                if (actionState.errorMessage != null) ...[
                  const SizedBox(height: AppSpacing.s12),
                  Container(
                    padding: const EdgeInsets.all(AppSpacing.s12),
                    decoration: BoxDecoration(
                      color: AppColors.error.withAlpha(38),
                      borderRadius: BorderRadius.circular(AppSpacing.s8),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.error_outline, color: AppColors.error, size: 20),
                        const SizedBox(width: AppSpacing.s8),
                        Expanded(
                          child: Text(
                            actionState.errorMessage!,
                            style: AppTextStyles.bodySmall.copyWith(
                              color: AppColors.error,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildProviderGrid() {
    final supportedAuthTypes = _getSupportedAuthTypes();

    return Wrap(
      spacing: AppSpacing.s12,
      runSpacing: AppSpacing.s12,
      children: supportedAuthTypes.keys.map((provider) {
        final isSelected = _selectedProvider == provider;

        return GestureDetector(
          onTap: () {
            setState(() {
              _selectedProvider = provider;
              final authTypes = supportedAuthTypes[provider]!;
              _selectedAuthType = authTypes.first;
            });
          },
          child: Container(
            width: (MediaQuery.of(context).size.width - AppSpacing.s24 * 2 - AppSpacing.s12 * 2) / 3,
            padding: const EdgeInsets.all(AppSpacing.s12),
            decoration: BoxDecoration(
              color: isSelected ? AppColors.primary.withAlpha(38) : AppColors.background,
              borderRadius: BorderRadius.circular(AppSpacing.s12),
              border: Border.all(
                color: isSelected ? AppColors.primary : AppColors.divider,
                width: isSelected ? 2 : 0.5,
              ),
            ),
            child: Column(
              children: [
                ConnectionProviderIcon.buildIcon(provider, size: 32),
                const SizedBox(height: AppSpacing.s8),
                Text(
                  _getProviderDisplayName(provider),
                  style: AppTextStyles.labelMedium.copyWith(
                    color: isSelected ? AppColors.primary : AppColors.textPrimary,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _buildAuthTypeSelection() {
    final supportedAuthTypes = _getSupportedAuthTypes()[_selectedProvider]!;

    if (supportedAuthTypes.length == 1) {
      return Container(
        padding: const EdgeInsets.all(AppSpacing.s12),
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(AppSpacing.s12),
          border: Border.all(color: AppColors.divider),
        ),
        child: Text(
          _getAuthTypeDisplayName(supportedAuthTypes.first),
          style: AppTextStyles.bodyMedium,
        ),
      );
    }

    return Wrap(
      spacing: AppSpacing.s8,
      children: supportedAuthTypes.map((authType) {
        final isSelected = _selectedAuthType == authType;

        return ChoiceChip(
          label: Text(_getAuthTypeDisplayName(authType)),
          selected: isSelected,
          onSelected: (selected) {
            setState(() {
              _selectedAuthType = authType;
            });
          },
          selectedColor: AppColors.primary.withAlpha(77),
          backgroundColor: AppColors.background,
          labelStyle: AppTextStyles.labelMedium.copyWith(
            color: isSelected ? AppColors.primary : AppColors.textSecondary,
          ),
        );
      }).toList(),
    );
  }

  String _getProviderDisplayName(ConnectionProvider provider) {
    switch (provider) {
      case ConnectionProvider.claude:
        return 'Claude';
      case ConnectionProvider.slack:
        return 'Slack';
      case ConnectionProvider.gmail:
        return 'Gmail';
      case ConnectionProvider.github:
        return 'GitHub';
      case ConnectionProvider.discord:
        return 'Discord';
      case ConnectionProvider.jira:
        return 'Jira';
      case ConnectionProvider.linear:
        return 'Linear';
      case ConnectionProvider.teams:
        return 'Teams';
    }
  }

  String _getAuthTypeDisplayName(ConnectionAuthType authType) {
    switch (authType) {
      case ConnectionAuthType.apiKey:
        return 'API Key';
      case ConnectionAuthType.oauth:
        return 'OAuth';
      case ConnectionAuthType.signature:
        return 'Signature';
      case ConnectionAuthType.system:
        return 'System';
    }
  }

  Future<void> _handleCreate() async {
    if (_selectedProvider == null || _selectedAuthType == null) {
      return;
    }

    final displayName = _displayNameController.text.trim();
    if (displayName.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a display name')),
      );
      return;
    }

    if (_selectedAuthType == ConnectionAuthType.oauth) {
      // Start OAuth flow
      await ref.read(oauthStateProvider.notifier).startOAuth(
            provider: _selectedProvider!,
            displayName: displayName,
          );

      // TODO: Open OAuth URL in browser
      final oauthState = ref.read(oauthStateProvider);
      if (oauthState.authorizationUrl != null) {
        // TODO: Launch URL in browser
        // For now, just show a message
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('OAuth flow not fully implemented yet')),
          );
        }
      }
    } else {
      // Create API Key connection
      await ref.read(connectionActionsProvider.notifier).createConnection(
            provider: _selectedProvider!,
            authType: _selectedAuthType!,
            displayName: displayName,
          );
    }
  }
}
