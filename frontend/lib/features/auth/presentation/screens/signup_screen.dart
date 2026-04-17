import 'package:flutter/material.dart';

/// Signup screen
class SignupScreen extends StatelessWidget {
  const SignupScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('회원가입'),
      ),
      body: const Center(
        child: Text('SignupScreen - Phase 3에서 구현 예정'),
      ),
    );
  }
}
