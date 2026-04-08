import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter/foundation.dart';

/// Service for managing local notifications
class LocalNotificationService {
  static final LocalNotificationService _instance =
      LocalNotificationService._internal();
  factory LocalNotificationService() => _instance;
  LocalNotificationService._internal();

  final FlutterLocalNotificationsPlugin _flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();

  /// Callback for when a notification is tapped
  Function(String?)? onNotificationTapped;

  /// Initialize the local notification service
  Future<void> initialize() async {
    // Android initialization settings
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    // iOS initialization settings
    const DarwinInitializationSettings initializationSettingsIOS =
        DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    // Combined initialization settings
    const InitializationSettings initializationSettings =
        InitializationSettings(
      android: initializationSettingsAndroid,
      iOS: initializationSettingsIOS,
    );

    // Initialize the plugin
    await _flutterLocalNotificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: _onNotificationResponse,
    );

    // Create notification channels for Android
    if (!kIsWeb) {
      await _createNotificationChannels();
    }

    debugPrint('LocalNotificationService initialized');
  }

  /// Request notification permissions (iOS)
  Future<bool?> requestPermissions() async {
    if (kIsWeb) return true;

    return await _flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(
          alert: true,
          badge: true,
          sound: true,
        );
  }

  /// Create notification channels for Android
  Future<void> _createNotificationChannels() async {
    // High priority channel (URGENT notifications)
    const AndroidNotificationChannel urgentChannel =
        AndroidNotificationChannel(
      'urgent_notifications',
      'Urgent Notifications',
      description: 'Urgent notifications that require immediate attention',
      importance: Importance.max,
      playSound: true,
      enableVibration: true,
    );

    // Default channel (HIGH, MEDIUM notifications)
    const AndroidNotificationChannel defaultChannel =
        AndroidNotificationChannel(
      'default_notifications',
      'Notifications',
      description: 'General notifications from various sources',
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
    );

    // Low priority channel (LOW notifications)
    const AndroidNotificationChannel lowChannel = AndroidNotificationChannel(
      'low_notifications',
      'Low Priority Notifications',
      description: 'Low priority notifications',
      importance: Importance.low,
      playSound: false,
      enableVibration: false,
    );

    // Create channels
    await _flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(urgentChannel);

    await _flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(defaultChannel);

    await _flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(lowChannel);

    debugPrint('Notification channels created');
  }

  /// Handle notification tap
  void _onNotificationResponse(NotificationResponse response) {
    debugPrint('Notification tapped: ${response.payload}');
    onNotificationTapped?.call(response.payload);
  }

  /// Show a notification
  Future<void> showNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
    NotificationPriority priority = NotificationPriority.medium,
    NotificationSource source = NotificationSource.claude,
  }) async {
    // Determine channel and importance based on priority
    String channelId;
    Importance importance;
    Priority androidPriority;

    switch (priority) {
      case NotificationPriority.urgent:
        channelId = 'urgent_notifications';
        importance = Importance.max;
        androidPriority = Priority.max;
        break;
      case NotificationPriority.high:
        channelId = 'default_notifications';
        importance = Importance.high;
        androidPriority = Priority.high;
        break;
      case NotificationPriority.medium:
        channelId = 'default_notifications';
        importance = Importance.defaultImportance;
        androidPriority = Priority.defaultPriority;
        break;
      case NotificationPriority.low:
        channelId = 'low_notifications';
        importance = Importance.low;
        androidPriority = Priority.low;
        break;
    }

    // Android notification details
    final AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      channelId,
      channelId == 'urgent_notifications'
          ? 'Urgent Notifications'
          : channelId == 'default_notifications'
              ? 'Notifications'
              : 'Low Priority Notifications',
      channelDescription: _getChannelDescription(channelId),
      importance: importance,
      priority: androidPriority,
      icon: _getSourceIcon(source),
      playSound: priority != NotificationPriority.low,
      enableVibration: priority != NotificationPriority.low,
    );

    // iOS notification details
    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    // Combined notification details
    final NotificationDetails notificationDetails = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    // Show notification
    await _flutterLocalNotificationsPlugin.show(
      id,
      title,
      body,
      notificationDetails,
      payload: payload,
    );

    debugPrint('Notification shown: $title');
  }

  /// Get channel description
  String _getChannelDescription(String channelId) {
    switch (channelId) {
      case 'urgent_notifications':
        return 'Urgent notifications that require immediate attention';
      case 'default_notifications':
        return 'General notifications from various sources';
      case 'low_notifications':
        return 'Low priority notifications';
      default:
        return '';
    }
  }

  /// Get source-specific icon
  String _getSourceIcon(NotificationSource source) {
    // For now, use default launcher icon
    // In the future, add custom icons for each source
    return '@mipmap/ic_launcher';
  }

  /// Cancel a notification
  Future<void> cancelNotification(int id) async {
    await _flutterLocalNotificationsPlugin.cancel(id);
  }

  /// Cancel all notifications
  Future<void> cancelAllNotifications() async {
    await _flutterLocalNotificationsPlugin.cancelAll();
  }

  /// Get active notifications (Android 6.0+)
  Future<List<ActiveNotification>> getActiveNotifications() async {
    if (kIsWeb) return [];

    final List<ActiveNotification>? activeNotifications =
        await _flutterLocalNotificationsPlugin
            .resolvePlatformSpecificImplementation<
                AndroidFlutterLocalNotificationsPlugin>()
            ?.getActiveNotifications();

    return activeNotifications ?? [];
  }
}

/// Notification priority levels
enum NotificationPriority {
  low,
  medium,
  high,
  urgent,
}

/// Notification sources
enum NotificationSource {
  claude,
  slack,
  github,
  gmail,
}
