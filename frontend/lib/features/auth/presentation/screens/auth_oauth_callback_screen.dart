import 'package:flutter/material.dart';

/// OAuth callback screen
class AuthOAuthCallbackScreen extends StatelessWidget {
  const AuthOAuthCallbackScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('소셜 로그인 처리'),
      ),
      body: const Center(
        child: CircularProgressIndicator(),
      ),
    );
  }
}
