import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';

/// Chat screen (Phase 2)
class ChatScreen extends StatelessWidget {
  const ChatScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Chat'),
      ),
      body: const Center(
        child: Text(
          'Chat Screen\n(Phase 2)',
          style: AppTextStyles.headlineMedium,
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
