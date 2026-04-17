import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/domain/entities/auth_platform.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_platform_provider.dart';

void main() {
  group('OAuthCallbackUriResolver', () {
    const resolver = OAuthCallbackUriResolver();

    test('builds a hash-based callback URL for web routing', () {
      final redirectUri = resolver.resolve(
        platform: AuthPlatform.web,
        baseUri: Uri.parse('https://app.notio.dev/login'),
      );

      expect(redirectUri, 'https://app.notio.dev/#/auth/oauth/callback');
    });

    test('builds the native callback scheme for mobile platforms', () {
      expect(
        resolver.resolve(platform: AuthPlatform.android),
        'notio://auth/oauth/callback',
      );
      expect(
        resolver.resolve(platform: AuthPlatform.ios),
        'notio://auth/oauth/callback',
      );
    });
  });
}
