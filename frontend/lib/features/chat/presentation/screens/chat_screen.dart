import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/chat/presentation/providers/chat_providers.dart';
import 'package:notio_app/features/chat/presentation/widgets/chat_input_field.dart';
import 'package:notio_app/features/chat/presentation/widgets/chat_message_bubble.dart';
import 'package:notio_app/features/chat/presentation/widgets/daily_summary_card.dart';
import 'package:notio_app/features/chat/presentation/widgets/streaming_message_bubble.dart';

/// Chat screen (Phase 2)
class ChatScreen extends ConsumerStatefulWidget {
  const ChatScreen({super.key});

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final ScrollController _scrollController = ScrollController();
  bool _isSummaryExpanded = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => _scrollToBottom(jump: true),
    );
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom({bool jump = false}) {
    if (_scrollController.hasClients) {
      final offset = _scrollController.position.maxScrollExtent;
      if (jump) {
        _scrollController.jumpTo(offset);
      } else {
        _scrollController.animateTo(
          offset,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    }
  }

  void _scheduleScrollToBottom({bool jump = false}) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      _scrollToBottom(jump: jump);
    });
  }

  void _handleSendMessage(String content) {
    final notifier = ref.read(chatProvider.notifier);
    notifier.sendMessageWithStreaming(content);

    // Scroll to bottom after message is sent
    Future.delayed(
      const Duration(milliseconds: 100),
      () => _scheduleScrollToBottom(),
    );
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(chatProvider, (previous, next) {
      final previousMessageCount = previous?.messages.length ?? 0;
      final didInitialLoad = previous?.isLoading == true && !next.isLoading;
      final didMessageCountChange = previousMessageCount != next.messages.length;
      final didStreamingContentChange =
          previous?.streamingContent != next.streamingContent &&
              next.streamingContent != null;

      if (didInitialLoad || didMessageCountChange || didStreamingContentChange) {
        _scheduleScrollToBottom(jump: didInitialLoad);
      }
    });

    final chatState = ref.watch(chatProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Chat'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.read(chatProvider.notifier).refresh();
            },
            tooltip: 'Refresh messages',
          ),
        ],
      ),
      body: Column(
        children: [
          // Daily Summary Card (collapsible)
          DailySummaryCard(
            isExpanded: _isSummaryExpanded,
            onToggle: () {
              setState(() {
                _isSummaryExpanded = !_isSummaryExpanded;
              });
            },
          ),

          // Messages List
          Expanded(
            child: _buildMessagesList(chatState),
          ),

          // Input Field
          ChatInputField(
            onSend: _handleSendMessage,
            enabled: !chatState.isSending && !chatState.isStreaming,
          ),
        ],
      ),
    );
  }

  Widget _buildMessagesList(chatState) {
    if (chatState.isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    // Only show full-screen error if initial load failed and no messages
    if (chatState.error != null && chatState.messages.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 48,
              color: AppColors.error,
            ),
            const SizedBox(height: AppSpacing.s16),
            Text(
              'Error loading messages',
              style: AppTextStyles.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.s8),
            Text(
              chatState.error!,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.s16),
            ElevatedButton(
              onPressed: () {
                ref.read(chatProvider.notifier).refresh();
              },
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    if (chatState.messages.isEmpty && !chatState.isStreaming) {
      return _buildEmptyState();
    }

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.s16),
      itemCount: chatState.messages.length + (chatState.isStreaming ? 1 : 0),
      itemBuilder: (context, index) {
        // Show streaming message at the end
        if (chatState.isStreaming && index == chatState.messages.length) {
          return StreamingMessageBubble(
            content: chatState.streamingContent ?? '',
          );
        }

        final message = chatState.messages[index];
        return ChatMessageBubble(message: message);
      },
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.s32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: AppColors.primaryGradient.scale(0.3),
              ),
              child: const Icon(
                Icons.chat_bubble_outline,
                size: 40,
                color: AppColors.primary,
              ),
            ),
            const SizedBox(height: AppSpacing.s24),
            Text(
              'Start a conversation',
              style: AppTextStyles.headlineMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.s8),
            Text(
              'Ask me anything about your notifications,\nprojects, or get help with your tasks.',
              style: AppTextStyles.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.s32),
            _buildExampleChips(),
          ],
        ),
      ),
    );
  }

  Widget _buildExampleChips() {
    final examples = [
      'What are my important notifications?',
      'Summarize today\'s messages',
      'Help me prioritize tasks',
    ];

    return Wrap(
      spacing: AppSpacing.s8,
      runSpacing: AppSpacing.s8,
      alignment: WrapAlignment.center,
      children: examples.map((example) {
        return ActionChip(
          label: Text(example),
          onPressed: () => _handleSendMessage(example),
          backgroundColor: AppColors.surfaceLight,
          labelStyle: AppTextStyles.caption.copyWith(
            color: AppColors.textSecondary,
          ),
        );
      }).toList(),
    );
  }
}
