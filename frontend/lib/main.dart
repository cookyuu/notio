import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/network/connectivity_service.dart';
import 'package:notio_app/core/network/sync_service.dart';
import 'package:notio_app/core/services/realtime_notification_service.dart';
import 'package:notio_app/core/router/app_router.dart';
import 'package:notio_app/core/theme/app_theme.dart';
import 'package:notio_app/features/settings/presentation/providers/settings_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timeago/timeago.dart' as timeago;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  timeago.setLocaleMessages('ko', timeago.KoMessages());

  final sharedPreferences = await SharedPreferences.getInstance();

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(sharedPreferences),
      ],
      child: const NotioApp(),
    ),
  );
}

class NotioApp extends ConsumerStatefulWidget {
  const NotioApp({super.key});

  @override
  ConsumerState<NotioApp> createState() => _NotioAppState();
}

class _NotioAppState extends ConsumerState<NotioApp> {
  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(connectivityServiceProvider);
      ref.read(syncServiceProvider);
      ref.read(realtimeNotificationServiceProvider);
    });
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
