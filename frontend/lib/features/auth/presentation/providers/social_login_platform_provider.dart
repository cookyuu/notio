import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/domain/entities/auth_platform.dart';

class OAuthCallbackUriResolver {
  const OAuthCallbackUriResolver();

  static const String _nativeScheme = 'notio';
  static const String _nativeHost = 'auth';
  static const String _nativeCallbackPath = '/oauth/callback';

  String resolve({
    required AuthPlatform platform,
    Uri? baseUri,
  }) {
    switch (platform) {
      case AuthPlatform.web:
        final resolvedBase = baseUri ?? Uri.base;
        return '${resolvedBase.origin}/#${Routes.authOAuthCallback}';
      case AuthPlatform.ios:
      case AuthPlatform.android:
        return Uri(
          scheme: _nativeScheme,
          host: _nativeHost,
          path: _nativeCallbackPath,
        ).toString();
    }
  }
}

final currentAuthPlatformProvider = Provider<AuthPlatform>((ref) {
  if (kIsWeb) {
    return AuthPlatform.web;
  }

  switch (defaultTargetPlatform) {
    case TargetPlatform.iOS:
    case TargetPlatform.macOS:
      return AuthPlatform.ios;
    case TargetPlatform.android:
      return AuthPlatform.android;
    default:
      return AuthPlatform.web;
  }
});

final oauthCallbackUriResolverProvider = Provider<OAuthCallbackUriResolver>(
  (ref) => const OAuthCallbackUriResolver(),
);

final oauthRedirectUriProvider = Provider<String>((ref) {
  final platform = ref.watch(currentAuthPlatformProvider);
  final resolver = ref.watch(oauthCallbackUriResolverProvider);
  return resolver.resolve(platform: platform);
});
