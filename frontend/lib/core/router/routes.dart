/// App route paths
class Routes {
  Routes._();

  // Auth routes
  static const String login = '/login';
  static const String signup = '/signup';
  static const String findId = '/find-id';
  static const String resetPasswordRequest = '/reset-password/request';
  static const String resetPasswordConfirm = '/reset-password/confirm';
  static const String authOAuthCallback = '/auth/oauth/callback';

  // Main routes
  static const String notifications = '/notifications';
  static const String chat = '/chat';
  static const String analytics = '/analytics';
  static const String settings = '/settings';
  static const String connections = '/settings/connections';
}
