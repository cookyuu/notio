import 'package:notio_app/core/router/routes.dart';

class AuthRoutePolicy {
  AuthRoutePolicy._();

  static bool isPublicRoute(String path) {
    return path == Routes.login ||
        path == Routes.signup ||
        path == Routes.findId ||
        path == Routes.resetPasswordRequest ||
        path == Routes.authOAuthCallback ||
        _isResetPasswordConfirmRoute(path);
  }

  static bool canAuthenticatedUserAccessAuthRoute({
    required String path,
    required Uri uri,
  }) {
    if (path == Routes.authOAuthCallback) {
      return true;
    }

    if (_isResetPasswordConfirmRoute(path) &&
        extractResetToken(uri: uri) != null) {
      return true;
    }

    return false;
  }

  static String? extractResetToken({
    required Uri uri,
    String? pathToken,
  }) {
    final directToken = _sanitizeToken(pathToken) ??
        _sanitizeToken(uri.queryParameters['token']) ??
        _sanitizeToken(uri.queryParameters['reset_token']) ??
        _extractFromFragment(uri.fragment);
    if (directToken != null) {
      return directToken;
    }

    final segments = <String>[];
    if (uri.scheme.isNotEmpty &&
        uri.scheme != 'http' &&
        uri.scheme != 'https' &&
        uri.host.isNotEmpty) {
      segments.add(uri.host);
    }
    segments.addAll(uri.pathSegments.where((segment) => segment.isNotEmpty));

    final confirmIndex = segments.lastIndexOf('confirm');
    if (confirmIndex != -1 && confirmIndex + 1 < segments.length) {
      return _sanitizeToken(segments[confirmIndex + 1]);
    }

    return null;
  }

  static bool _isResetPasswordConfirmRoute(String path) {
    return path == Routes.resetPasswordConfirm ||
        path.startsWith('${Routes.resetPasswordConfirm}/');
  }

  static String? _extractFromFragment(String fragment) {
    if (fragment.isEmpty) {
      return null;
    }

    final normalizedFragment =
        fragment.startsWith('/') ? fragment : '/$fragment';
    final fragmentUri = Uri.parse(normalizedFragment);

    return _sanitizeToken(fragmentUri.queryParameters['token']) ??
        _sanitizeToken(fragmentUri.queryParameters['reset_token']) ??
        extractResetToken(uri: fragmentUri);
  }

  static String? _sanitizeToken(String? token) {
    if (token == null) {
      return null;
    }

    final trimmed = token.trim();
    return trimmed.isEmpty ? null : trimmed;
  }
}
