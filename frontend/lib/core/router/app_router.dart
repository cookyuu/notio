import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/features/auth/domain/auth_route_policy.dart';
import 'package:notio_app/features/auth/presentation/providers/auth_session_notifier.dart';
import 'package:notio_app/features/auth/presentation/screens/login_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/signup_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/find_id_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_request_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/password_reset_confirm_screen.dart';
import 'package:notio_app/features/auth/presentation/screens/auth_oauth_callback_screen.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/notification/presentation/screens/notification_detail_screen.dart';
import 'package:notio_app/features/notification/presentation/screens/notifications_screen.dart';
import 'package:notio_app/features/analytics/presentation/analytics_screen.dart';
import 'package:notio_app/features/settings/presentation/settings_screen.dart';
import 'package:notio_app/features/connections/presentation/screens/connections_screen.dart';
import 'package:notio_app/features/connections/presentation/screens/connection_detail_screen.dart';
import 'package:notio_app/features/delivery_feed/presentation/screens/delivery_feed_screen.dart';
import 'package:notio_app/features/channels/presentation/screens/channels_screen.dart';
import 'package:notio_app/features/channels/presentation/screens/channel_create_screen.dart';
import 'package:notio_app/features/channels/presentation/screens/routing_rules_screen.dart';

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
    errorBuilder: (context, state) => Scaffold(
      backgroundColor: AppColors.bg0,
      appBar: AppBar(
        backgroundColor: AppColors.bg1,
        leading: BackButton(onPressed: () => context.go(Routes.notifications)),
        title: const Text('오류'),
      ),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: AppColors.error, size: 64),
            const SizedBox(height: AppSpacing.s16),
            const Text('페이지를 찾을 수 없습니다', style: AppTextStyles.headlineSmall),
            const SizedBox(height: AppSpacing.s24),
            FilledButton(
              onPressed: () => context.go(Routes.notifications),
              child: const Text('홈으로 이동'),
            ),
          ],
        ),
      ),
    ),
    redirect: (context, state) {
      final authState = ref.read(authSessionNotifierProvider);
      final isAuthenticated = authState.value?.isAuthenticated ?? false;
      final currentPath = state.matchedLocation;
      final isPublicRoute = AuthRoutePolicy.isPublicRoute(currentPath);

      // If not authenticated and not on public route, redirect to login
      if (!isAuthenticated && !isPublicRoute) {
        return Routes.login;
      }

      if (isAuthenticated &&
          isPublicRoute &&
          !AuthRoutePolicy.canAuthenticatedUserAccessAuthRoute(
            path: currentPath,
            uri: state.uri,
          )) {
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
        path: '${Routes.resetPasswordConfirm}/:token',
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
      GoRoute(
        path: Routes.channels,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: ChannelsScreen(),
        ),
        routes: [
          GoRoute(
            path: 'create',
            pageBuilder: (context, state) => const NoTransitionPage(
              child: ChannelCreateScreen(),
            ),
          ),
        ],
      ),
      GoRoute(
        path: Routes.routingRules,
        pageBuilder: (context, state) => const NoTransitionPage(
          child: RoutingRulesScreen(),
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
            routes: [
              GoRoute(
                path: ':id',
                pageBuilder: (context, state) {
                  final id = int.parse(state.pathParameters['id']!);
                  return NoTransitionPage(
                    child: NotificationDetailScreen(notificationId: id),
                  );
                },
              ),
            ],
          ),
          GoRoute(
            path: Routes.chat,
            pageBuilder: (context, state) => const NoTransitionPage(
              child: DeliveryFeedScreen(),
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
    final location = GoRouterState.of(context).uri.path;
    final isDetailRoute = RegExp(r'^/notifications/\d+$').hasMatch(location);
    return Scaffold(
      body: child,
      bottomNavigationBar: isDetailRoute ? null : const _BottomNavBar(),
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  const _BottomNavBar();

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).uri.toString();

    int getCurrentIndex() {
      if (location.startsWith(Routes.notifications)) return 0;
      if (location.startsWith(Routes.chat)) return 1;
      if (location == Routes.analytics) return 2;
      if (location == Routes.settings) return 3;
      return 0;
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
          icon: Icon(Icons.send_outlined),
          selectedIcon: Icon(Icons.send),
          label: 'Deliveries',
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

