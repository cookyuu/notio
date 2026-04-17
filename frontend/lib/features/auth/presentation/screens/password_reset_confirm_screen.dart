import 'package:flutter/material.dart';

/// Password reset confirm screen
class PasswordResetConfirmScreen extends StatelessWidget {
  const PasswordResetConfirmScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('비밀번호 재설정'),
      ),
      body: const Center(
        child: Text('PasswordResetConfirmScreen - Phase 3에서 구현 예정'),
      ),
    );
  }
}
