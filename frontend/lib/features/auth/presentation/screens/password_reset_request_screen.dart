import 'package:flutter/material.dart';

/// Password reset request screen
class PasswordResetRequestScreen extends StatelessWidget {
  const PasswordResetRequestScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('비밀번호 재설정 요청'),
      ),
      body: const Center(
        child: Text('PasswordResetRequestScreen - Phase 3에서 구현 예정'),
      ),
    );
  }
}
