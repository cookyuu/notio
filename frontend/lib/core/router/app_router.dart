import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_notifier.dart';
import 'package:notio_app/features/auth/presentation/screens/login_screen.dart';
import 'package:notio_app/features/notification/presentation/screens/notifications_screen.dart';
import 'package:notio_app/features/chat/presentation/screens/chat_screen.dart';
import 'package:notio_app/features/analytics/presentation/analytics_screen.dart';
import 'package:notio_app/features/settings/presentation/settings_screen.dart';
import 'package:notio_app/features/developer/presentation/developer_menu_screen.dart';

/// App router provider
final goRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authNotifierProvider);

  return GoRouter(
    initialLocation: Routes.login,
    redirect: (context, state) {
      final isAuthenticated = authState.value?.isAuthenticated ?? false;
      final isLoggingIn = state.matchedLocation == Routes.login;

      // If not authenticated and not on login page, redirect to login
      if (!isAuthenticated && !isLoggingIn) {
        return Routes.login;
      }

      // If authenticated and on login page, redirect to notifications
      if (isAuthenticated && isLoggingIn) {
        return Routes.notifications;
      }

      return null;
    },
    routes: [
      GoRoute(
        path: Routes.login,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: LoginScreen(),
        ),
      ),
      GoRoute(
        path: Routes.developer,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: DeveloperMenuScreen(),
        ),
      ),
      ShellRoute(
        builder: (context, state, child) {
          return _MainScaffold(child: child);
        },
        routes: [
          GoRoute(
            path: Routes.notifications,
            pageBuilder: (context, state) => const NoTransitionPage(
              child: NotificationsScreen(),
            ),
          ),
          GoRoute(
            path: Routes.chat,
            pageBuilder: (context, state) => const NoTransitionPage(
              child: ChatScreen(),
            ),
          ),
          GoRoute(
            path: Routes.analytics,
            pageBuilder: (context, state) => const NoTransitionPage(
              child: AnalyticsScreen(),
            ),
          ),
          GoRoute(
            path: Routes.settings,
            pageBuilder: (context, state) => const NoTransitionPage(
              child: SettingsScreen(),
            ),
          ),
        ],
      ),
    ],
  );
});

class _MainScaffold extends StatelessWidget {
  const _MainScaffold({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: child,
      bottomNavigationBar: const _BottomNavBar(),
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  const _BottomNavBar();

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).uri.toString();

    int getCurrentIndex() {
      switch (location) {
        case Routes.notifications:
          return 0;
        case Routes.chat:
          return 1;
        case Routes.analytics:
          return 2;
        case Routes.settings:
          return 3;
        default:
          return 0;
      }
    }

    return NavigationBar(
      selectedIndex: getCurrentIndex(),
      onDestinationSelected: (index) {
        switch (index) {
          case 0:
            context.go(Routes.notifications);
            break;
          case 1:
            context.go(Routes.chat);
            break;
          case 2:
            context.go(Routes.analytics);
            break;
          case 3:
            context.go(Routes.settings);
            break;
        }
      },
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.notifications_outlined),
          selectedIcon: Icon(Icons.notifications),
          label: 'Notifications',
        ),
        NavigationDestination(
          icon: Icon(Icons.chat_outlined),
          selectedIcon: Icon(Icons.chat),
          label: 'Chat',
        ),
        NavigationDestination(
          icon: Icon(Icons.analytics_outlined),
          selectedIcon: Icon(Icons.analytics),
          label: 'Analytics',
        ),
        NavigationDestination(
          icon: Icon(Icons.settings_outlined),
          selectedIcon: Icon(Icons.settings),
          label: 'Settings',
        ),
      ],
    );
  }
}
