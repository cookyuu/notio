import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_session_notifier.dart';
import 'package:notio_app/features/auth/presentation/screens/login_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/signup_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/find_id_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_request_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_confirm_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/auth_oauth_callback_screen.dart';
import 'package:notio_app/features/notification/presentation/screens/notifications_screen.dart';
import 'package:notio_app/features/chat/presentation/screens/chat_screen.dart';
import 'package:notio_app/features/analytics/presentation/analytics_screen.dart';
import 'package:notio_app/features/settings/presentation/settings_screen.dart';
import 'package:notio_app/features/settings/presentation/screens/developer_menu_screen.dart';
import 'package:notio_app/features/connections/presentation/screens/connections_screen.dart';
import 'package:notio_app/features/connections/presentation/screens/connection_detail_screen.dart';

class RouterRefreshNotifier extends ChangeNotifier {
  void refresh() {
    notifyListeners();
  }
}

/// App router provider
final goRouterProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = RouterRefreshNotifier();
  ref.onDispose(refreshNotifier.dispose);
  ref.listen(authSessionNotifierProvider, (_, __) {
    refreshNotifier.refresh();
  });

  return GoRouter(
    initialLocation: Routes.login,
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final authState = ref.read(authSessionNotifierProvider);
      final isAuthenticated = authState.value?.isAuthenticated ?? false;
      final currentPath = state.matchedLocation;

      // Define public routes that don't require authentication
      final publicRoutes = [
        Routes.login,
        Routes.signup,
        Routes.findId,
        Routes.resetPasswordRequest,
        Routes.resetPasswordConfirm,
        Routes.authOAuthCallback,
      ];

      final isPublicRoute = publicRoutes.contains(currentPath);

      // If not authenticated and not on public route, redirect to login
      if (!isAuthenticated && !isPublicRoute) {
        return Routes.login;
      }

      // If authenticated and on public route, redirect to notifications
      if (isAuthenticated && isPublicRoute) {
        return Routes.notifications;
      }

      return null;
    },
    routes: [
      // Auth routes
      GoRoute(
        path: Routes.login,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: LoginScreen(),
        ),
      ),
      GoRoute(
        path: Routes.signup,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: SignupScreen(),
        ),
      ),
      GoRoute(
        path: Routes.findId,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: FindIdScreen(),
        ),
      ),
      GoRoute(
        path: Routes.resetPasswordRequest,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: PasswordResetRequestScreen(),
        ),
      ),
      GoRoute(
        path: Routes.resetPasswordConfirm,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: PasswordResetConfirmScreen(),
        ),
      ),
      GoRoute(
        path: Routes.authOAuthCallback,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: AuthOAuthCallbackScreen(),
        ),
      ),
      GoRoute(
        path: Routes.developer,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: DeveloperMenuScreen(),
        ),
      ),
      GoRoute(
        path: Routes.connections,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: ConnectionsScreen(),
        ),
        routes: [
          GoRoute(
            path: ':id',
            pageBuilder: (context, state) {
              final id = int.parse(state.pathParameters['id']!);
              return NoTransitionPage(
                child: ConnectionDetailScreen(connectionId: id),
              );
            },
          ),
        ],
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
