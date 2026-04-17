import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';

class SocialLoginProviderConfig {
  const SocialLoginProviderConfig({
    required this.provider,
    required this.label,
    required this.icon,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.isEnabled,
  });

  final AuthProvider provider;
  final String label;
  final IconData icon;
  final Color backgroundColor;
  final Color foregroundColor;
  final bool isEnabled;
}

final socialLoginProviderConfigsProvider =
    Provider<List<SocialLoginProviderConfig>>((ref) {
  return const [
    SocialLoginProviderConfig(
      provider: AuthProvider.google,
      label: 'Google',
      icon: Icons.public,
      backgroundColor: Color(0xFFF5F7FF),
      foregroundColor: Color(0xFF111827),
      isEnabled: bool.fromEnvironment(
        'NOTIO_AUTH_GOOGLE_ENABLED',
        defaultValue: true,
      ),
    ),
    SocialLoginProviderConfig(
      provider: AuthProvider.apple,
      label: 'Apple',
      icon: Icons.apple,
      backgroundColor: Color(0xFF111111),
      foregroundColor: AppColors.text1,
      isEnabled: bool.fromEnvironment(
        'NOTIO_AUTH_APPLE_ENABLED',
        defaultValue: true,
      ),
    ),
    SocialLoginProviderConfig(
      provider: AuthProvider.kakao,
      label: 'Kakao',
      icon: Icons.chat_bubble,
      backgroundColor: Color(0xFFFEE500),
      foregroundColor: Color(0xFF191919),
      isEnabled: bool.fromEnvironment(
        'NOTIO_AUTH_KAKAO_ENABLED',
        defaultValue: true,
      ),
    ),
    SocialLoginProviderConfig(
      provider: AuthProvider.naver,
      label: 'Naver',
      icon: Icons.language,
      backgroundColor: Color(0xFF03C75A),
      foregroundColor: Colors.white,
      isEnabled: bool.fromEnvironment(
        'NOTIO_AUTH_NAVER_ENABLED',
        defaultValue: true,
      ),
    ),
  ].where((config) => config.isEnabled).toList(growable: false);
});
