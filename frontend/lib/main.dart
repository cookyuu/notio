import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/router/app_router.dart';
import 'package:notio_app/core/theme/app_theme.dart';

void main() {
  runApp(
    const ProviderScope(
      child: NotioApp(),
    ),
  );
}

class NotioApp extends StatelessWidget {
  const NotioApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Notio',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      routerConfig: AppRouter.router,
    );
  }
}
