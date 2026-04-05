import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';

/// Notifications screen (Phase 1)
class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
      ),
      body: const Center(
        child: Text(
          'Notifications Screen\n(Phase 1)',
          style: AppTextStyles.headlineMedium,
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
