import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';

/// Settings screen (Phase 3)
class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: const Center(
        child: Text(
          'Settings Screen\n(Phase 3)',
          style: AppTextStyles.headlineMedium,
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
