import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/router/app_router.dart';
import 'package:notio_app/core/theme/app_theme.dart';
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

    return MaterialApp.router(
      title: 'Notio',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      routerConfig: router,
    );
  }
}
