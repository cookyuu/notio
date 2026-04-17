import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/features/auth/domain/entities/auth_platform.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';
import 'package:notio_app/features/auth/domain/entities/social_login_launch_mode.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_entry_strategy_provider.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_platform_provider.dart';

class SocialLoginClientConfig {
  const SocialLoginClientConfig({
    this.webClientId,
    this.androidClientId,
    this.iosClientId,
  });

  final String? webClientId;
  final String? androidClientId;
  final String? iosClientId;

  String? clientIdFor(AuthPlatform platform) {
    switch (platform) {
      case AuthPlatform.web:
        return _normalize(webClientId);
      case AuthPlatform.android:
        return _normalize(androidClientId);
      case AuthPlatform.ios:
        return _normalize(iosClientId);
    }
  }

  bool isConfiguredFor(AuthPlatform platform) {
    return clientIdFor(platform) != null;
  }

  String? _normalize(String? value) {
    final trimmed = value?.trim();
    return trimmed == null || trimmed.isEmpty ? null : trimmed;
  }
}

class SocialLoginProviderConfig {
  const SocialLoginProviderConfig({
    required this.provider,
    required this.label,
    required this.icon,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.clientConfig,
    required this.launchMode,
  });

  final AuthProvider provider;
  final String label;
  final IconData icon;
  final Color backgroundColor;
  final Color foregroundColor;
  final SocialLoginClientConfig clientConfig;
  final SocialLoginLaunchMode launchMode;

  bool isVisibleOn(AuthPlatform platform) {
    return clientConfig.isConfiguredFor(platform);
  }
}

final socialLoginProviderConfigsProvider =
    Provider<List<SocialLoginProviderConfig>>((ref) {
  final platform = ref.watch(currentAuthPlatformProvider);

  final configs = [
    SocialLoginProviderConfig(
      provider: AuthProvider.google,
      label: 'Google',
      icon: Icons.public,
      backgroundColor: const Color(0xFFF5F7FF),
      foregroundColor: const Color(0xFF111827),
      clientConfig: const SocialLoginClientConfig(
        webClientId: String.fromEnvironment('NOTIO_AUTH_GOOGLE_WEB_CLIENT_ID'),
        androidClientId: String.fromEnvironment(
          'NOTIO_AUTH_GOOGLE_ANDROID_CLIENT_ID',
        ),
        iosClientId: String.fromEnvironment('NOTIO_AUTH_GOOGLE_IOS_CLIENT_ID'),
      ),
      launchMode: ref
          .watch(
            socialLoginEntryStrategyProvider(AuthProvider.google),
          )
          .launchMode,
    ),
    SocialLoginProviderConfig(
      provider: AuthProvider.apple,
      label: 'Apple',
      icon: Icons.apple,
      backgroundColor: const Color(0xFF111111),
      foregroundColor: AppColors.text1,
      clientConfig: const SocialLoginClientConfig(
        webClientId: String.fromEnvironment('NOTIO_AUTH_APPLE_WEB_CLIENT_ID'),
        iosClientId: String.fromEnvironment('NOTIO_AUTH_APPLE_IOS_CLIENT_ID'),
      ),
      launchMode: ref
          .watch(
            socialLoginEntryStrategyProvider(AuthProvider.apple),
          )
          .launchMode,
    ),
    SocialLoginProviderConfig(
      provider: AuthProvider.kakao,
      label: 'Kakao',
      icon: Icons.chat_bubble,
      backgroundColor: const Color(0xFFFEE500),
      foregroundColor: const Color(0xFF191919),
      clientConfig: const SocialLoginClientConfig(
        webClientId: String.fromEnvironment('NOTIO_AUTH_KAKAO_WEB_CLIENT_ID'),
        androidClientId: String.fromEnvironment(
          'NOTIO_AUTH_KAKAO_ANDROID_CLIENT_ID',
        ),
        iosClientId: String.fromEnvironment('NOTIO_AUTH_KAKAO_IOS_CLIENT_ID'),
      ),
      launchMode: ref
          .watch(
            socialLoginEntryStrategyProvider(AuthProvider.kakao),
          )
          .launchMode,
    ),
    SocialLoginProviderConfig(
      provider: AuthProvider.naver,
      label: 'Naver',
      icon: Icons.language,
      backgroundColor: const Color(0xFF03C75A),
      foregroundColor: Colors.white,
      clientConfig: const SocialLoginClientConfig(
        webClientId: String.fromEnvironment('NOTIO_AUTH_NAVER_WEB_CLIENT_ID'),
        androidClientId: String.fromEnvironment(
          'NOTIO_AUTH_NAVER_ANDROID_CLIENT_ID',
        ),
        iosClientId: String.fromEnvironment('NOTIO_AUTH_NAVER_IOS_CLIENT_ID'),
      ),
      launchMode: ref
          .watch(
            socialLoginEntryStrategyProvider(AuthProvider.naver),
          )
          .launchMode,
    ),
  ];

  return configs
      .where((config) => config.isVisibleOn(platform))
      .toList(growable: false);
});
