import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';
import 'package:notio_app/features/auth/domain/entities/social_login_launch_mode.dart';
import 'package:url_launcher/url_launcher.dart';

abstract interface class SocialLoginEntryStrategy {
  SocialLoginLaunchMode get launchMode;

  Future<bool> launchAuthorizationUrl(String authorizationUrl);
}

class RedirectSocialLoginEntryStrategy implements SocialLoginEntryStrategy {
  const RedirectSocialLoginEntryStrategy();

  @override
  SocialLoginLaunchMode get launchMode => SocialLoginLaunchMode.redirect;

  @override
  Future<bool> launchAuthorizationUrl(String authorizationUrl) {
    return launchUrl(
      Uri.parse(authorizationUrl),
      mode: LaunchMode.externalApplication,
    );
  }
}

final socialLoginEntryStrategyProvider =
    Provider.family<SocialLoginEntryStrategy, AuthProvider>((ref, provider) {
  switch (provider) {
    case AuthProvider.google:
    case AuthProvider.apple:
    case AuthProvider.kakao:
    case AuthProvider.naver:
      return const RedirectSocialLoginEntryStrategy();
    case AuthProvider.local:
      throw UnsupportedError('Local auth does not support social login entry.');
  }
});
