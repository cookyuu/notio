/// API endpoint constants
class ApiConstants {
  ApiConstants._();

  // Base URL - should be configured via environment variables in production
  static const baseUrl = 'http://localhost:8080';

  // API version
  static const apiVersion = '/api/v1';

  // Notification endpoints
  static const notifications = '$apiVersion/notifications';
  static const notificationById = '$apiVersion/notifications/{id}';
  static const notificationMarkAsRead = '$apiVersion/notifications/{id}/read';
  static const notificationMarkAllAsRead = '$apiVersion/notifications/read-all';
  static const notificationUnreadCount = '$apiVersion/notifications/unread-count';

  // Chat endpoints
  static const chat = '$apiVersion/chat';
  static const chatStream = '$apiVersion/chat/stream';
  static const dailySummary = '$apiVersion/chat/daily-summary';

  // Analytics endpoints
  static const analytics = '$apiVersion/analytics';
  static const weeklyAnalytics = '$apiVersion/analytics/weekly';

  // Todo endpoints
  static const todos = '$apiVersion/todos';
  static const todoById = '$apiVersion/todos/{id}';

  // Device endpoints
  static const deviceRegister = '$apiVersion/devices/register';
}
