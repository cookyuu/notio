import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/network/connectivity_service.dart';
import 'package:notio_app/core/network/sync_service.dart';
import 'package:notio_app/core/router/app_router.dart';
import 'package:notio_app/core/services/local_notification_service.dart';
import 'package:notio_app/core/theme/app_theme.dart';
import 'package:notio_app/features/settings/presentation/providers/settings_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timeago/timeago.dart' as timeago;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize timeago Korean locale
  timeago.setLocaleMessages('ko', timeago.KoMessages());

  // Initialize SharedPreferences
  final sharedPreferences = await SharedPreferences.getInstance();

  // Initialize Local Notifications
  final localNotificationService = LocalNotificationService();
  await localNotificationService.initialize();

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(sharedPreferences),
        localNotificationServiceProvider
            .overrideWithValue(localNotificationService),
      ],
      child: const NotioApp(),
    ),
  );
}

/// Provider for LocalNotificationService
final localNotificationServiceProvider =
    Provider<LocalNotificationService>((ref) {
  throw UnimplementedError('localNotificationServiceProvider not overridden');
});

class NotioApp extends ConsumerStatefulWidget {
  const NotioApp({super.key});

  @override
  ConsumerState<NotioApp> createState() => _NotioAppState();
}

class _NotioAppState extends ConsumerState<NotioApp> {
  @override
  void initState() {
    super.initState();

    // Set up notification tap handler
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final notificationService = ref.read(localNotificationServiceProvider);
      notificationService.onNotificationTapped = _handleNotificationTap;

      // Initialize connectivity service (auto-starts network monitoring)
      ref.read(connectivityServiceProvider);

      // Initialize sync service (auto-listens to network changes)
      ref.read(syncServiceProvider);
    });
  }

  void _handleNotificationTap(String? payload) {
    if (payload == null) return;

    debugPrint('Notification tapped with payload: $payload');

    // Get the router
    final router = ref.read(goRouterProvider);

    // For now, simply navigate to notifications screen
    // In the future, parse payload and navigate to specific screen
    router.go('/notifications');

    // TODO: Implement notification detail modal
    // TODO: Auto-mark notification as read
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(goRouterProvider);
    final isDarkMode = ref.watch(isDarkModeProvider);

    return MaterialApp.router(
      title: 'Notio',
      debugShowCheckedModeBanner: false,
      theme: isDarkMode ? AppTheme.darkTheme : AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: isDarkMode ? ThemeMode.dark : ThemeMode.light,
      routerConfig: router,
    );
  }
}
