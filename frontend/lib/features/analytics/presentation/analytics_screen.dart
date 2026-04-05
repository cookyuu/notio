import 'package:flutter/material.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';

/// Analytics screen (Phase 3)
class AnalyticsScreen extends StatelessWidget {
  const AnalyticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Analytics'),
      ),
      body: const Center(
        child: Text(
          'Analytics Screen\n(Phase 3)',
          style: AppTextStyles.headlineMedium,
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
