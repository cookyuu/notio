import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/domain/entities/auth_platform.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_config_provider.dart';

void main() {
  group('SocialLoginClientConfig', () {
    test('treats blank client ids as not configured', () {
      const config = SocialLoginClientConfig(
        webClientId: '   ',
        androidClientId: '',
        iosClientId: null,
      );

      expect(config.isConfiguredFor(AuthPlatform.web), isFalse);
      expect(config.isConfiguredFor(AuthPlatform.android), isFalse);
      expect(config.isConfiguredFor(AuthPlatform.ios), isFalse);
    });

    test('returns only the configured client id for a platform', () {
      const config = SocialLoginClientConfig(
        webClientId: 'web-client',
        androidClientId: 'android-client',
      );

      expect(config.clientIdFor(AuthPlatform.web), 'web-client');
      expect(config.clientIdFor(AuthPlatform.android), 'android-client');
      expect(config.clientIdFor(AuthPlatform.ios), isNull);
    });
  });
}
