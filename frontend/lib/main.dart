import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/router/app_router.dart';
import 'package:notio_app/core/theme/app_theme.dart';
import 'package:notio_app/features/settings/presentation/providers/settings_providers.dart';
import 'package:timeago/timeago.dart' as timeago;

void main() {
  // Initialize timeago Korean locale
  timeago.setLocaleMessages('ko', timeago.KoMessages());

  runApp(
    const ProviderScope(
      child: NotioApp(),
    ),
  );
}

class NotioApp extends ConsumerWidget {
  const NotioApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
