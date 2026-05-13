import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/channels/presentation/providers/channel_providers.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

class ChannelCreateScreen extends ConsumerStatefulWidget {
  const ChannelCreateScreen({super.key});

  @override
  ConsumerState<ChannelCreateScreen> createState() =>
      _ChannelCreateScreenState();
}

class _ChannelCreateScreenState extends ConsumerState<ChannelCreateScreen> {
  int _step = 0;
  ChannelTypeEnum? _selectedType;
  final _displayNameController = TextEditingController();
  final _credentialController = TextEditingController();
  final _targetIdController = TextEditingController();
  bool _isValidating = false;
  String? _error;

  @override
  void dispose() {
    _displayNameController.dispose();
    _credentialController.dispose();
    _targetIdController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('채널 추가'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (_step > 0) {
              setState(() => _step--);
            } else {
              context.pop();
            }
          },
        ),
      ),
      body: _buildStepContent(),
    );
  }

  Widget _buildStepContent() {
    return switch (_step) {
      0 => _buildStep1(),
      1 => _buildStep2(),
      2 => _buildStep3(),
      _ => const SizedBox.shrink(),
    };
  }

  Widget _buildStep1() {
    final types = [
      (
        ChannelTypeEnum.slack,
        'Slack',
        Icons.chat_bubble,
        const Color(0xFF4A154B)
      ),
      (
        ChannelTypeEnum.telegram,
        'Telegram',
        Icons.send,
        const Color(0xFF0088CC)
      ),
      (
        ChannelTypeEnum.discord,
        'Discord',
        Icons.headset,
        const Color(0xFF5865F2)
      ),
    ];

    return Padding(
      padding: const EdgeInsets.all(AppSpacing.s24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('채널 타입 선택', style: AppTextStyles.headlineMedium),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '연결할 메시징 플랫폼을 선택하세요',
            style: AppTextStyles.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.s24),
          ...types.map((t) {
            final (type, label, icon, color) = t;
            return Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.s12),
              child: InkWell(
                onTap: () => setState(() {
                  _selectedType = type;
                  _step = 1;
                }),
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  padding: const EdgeInsets.all(AppSpacing.s16),
                  decoration: BoxDecoration(
                    color: AppColors.surface,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.divider),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: color,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Icon(icon, color: Colors.white, size: 24),
                      ),
                      const SizedBox(width: AppSpacing.s16),
                      Text(label, style: AppTextStyles.titleMedium),
                      const Spacer(),
                      const Icon(
                          Icons.chevron_right, color: AppColors.textTertiary),
                    ],
                  ),
                ),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildStep2() {
    if (_selectedType == null) return const SizedBox.shrink();

    final isSlack = _selectedType == ChannelTypeEnum.slack;
    final isTelegram = _selectedType == ChannelTypeEnum.telegram;
    final isDiscord = _selectedType == ChannelTypeEnum.discord;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.s24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('자격증명 입력', style: AppTextStyles.headlineMedium),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '${_selectedType!.displayName} 채널 연결에 필요한 정보를 입력하세요',
            style: AppTextStyles.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.s24),
          TextField(
            controller: _displayNameController,
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(
              labelText: '채널 표시 이름',
              hintText: 'My Channel',
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          if (isSlack) ...[
            TextField(
              controller: _credentialController,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                labelText: 'Bot Token',
                hintText: 'xoxb-...',
              ),
              obscureText: true,
            ),
            const SizedBox(height: AppSpacing.s16),
            TextField(
              controller: _targetIdController,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                labelText: 'Channel ID',
                hintText: 'C0...',
              ),
            ),
          ],
          if (isTelegram) ...[
            TextField(
              controller: _credentialController,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                labelText: 'Bot Token',
                hintText: '123456:ABC-...',
              ),
              obscureText: true,
            ),
            const SizedBox(height: AppSpacing.s16),
            TextField(
              controller: _targetIdController,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                labelText: 'Chat ID',
                hintText: '-100...',
              ),
            ),
          ],
          if (isDiscord) ...[
            TextField(
              controller: _credentialController,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                labelText: 'Webhook URL',
                hintText: 'https://discord.com/api/webhooks/...',
              ),
              obscureText: true,
            ),
          ],
          const SizedBox(height: AppSpacing.s32),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _canProceedStep2()
                  ? () => setState(() => _step = 2)
                  : null,
              child: const Text('다음'),
            ),
          ),
        ],
      ),
    );
  }

  bool _canProceedStep2() {
    if (_displayNameController.text.trim().isEmpty) return false;
    if (_credentialController.text.trim().isEmpty) return false;
    if (_selectedType != ChannelTypeEnum.discord &&
        _targetIdController.text.trim().isEmpty) {
      return false;
    }
    return true;
  }

  Widget _buildStep3() {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.s24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('검증 및 저장', style: AppTextStyles.headlineMedium),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '입력한 정보를 검증하고 채널을 저장합니다',
            style: AppTextStyles.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.s32),
          if (_isValidating)
            const Center(
              child: Column(
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: AppSpacing.s16),
                  Text('검증 중...'),
                ],
              ),
            )
          else ...[
            if (_error != null) ...[
              Container(
                padding: const EdgeInsets.all(AppSpacing.s12),
                decoration: BoxDecoration(
                  color: AppColors.error.withAlpha(30),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppColors.error.withAlpha(80)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: AppColors.error),
                    const SizedBox(width: AppSpacing.s8),
                    Expanded(
                      child: Text(
                        _error!,
                        style: const TextStyle(color: AppColors.error),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.s16),
            ],
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _submit,
                child: const Text('채널 저장'),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _submit() async {
    setState(() {
      _isValidating = true;
      _error = null;
    });

    final success =
        await ref.read(channelNotifierProvider.notifier).createChannel(
              displayName: _displayNameController.text.trim(),
              channelType: _selectedType!.apiValue,
              credentialPlaintext: _credentialController.text.trim(),
              targetIdentifier: _selectedType != ChannelTypeEnum.discord
                  ? _targetIdController.text.trim()
                  : null,
            );

    if (!mounted) return;

    if (success) {
      context.pop();
    } else {
      setState(() {
        _isValidating = false;
        _error = ref.read(channelNotifierProvider).error;
      });
    }
  }
}
