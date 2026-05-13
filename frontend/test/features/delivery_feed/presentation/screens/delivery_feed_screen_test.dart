import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:notio_app/core/theme/app_theme.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/delivery_feed_item_entity.dart';
import 'package:notio_app/features/delivery_feed/domain/repository/delivery_feed_repository.dart';
import 'package:notio_app/features/delivery_feed/presentation/providers/delivery_feed_notifier.dart';
import 'package:notio_app/features/delivery_feed/presentation/screens/delivery_feed_screen.dart';
import 'package:notio_app/features/delivery_feed/presentation/widgets/delivery_bubble.dart';

// 로딩이 완료되지 않는 Fake — 로딩 상태 테스트용
class _PendingRepository implements DeliveryFeedRepository {
  @override
  Future<List<DeliveryFeedItemEntity>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  }) =>
      Completer<List<DeliveryFeedItemEntity>>().future;
}

// 빈 목록 반환 Fake
class _EmptyRepository implements DeliveryFeedRepository {
  @override
  Future<List<DeliveryFeedItemEntity>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  }) async =>
      [];
}

// 항목 반환 Fake
class _ItemsRepository implements DeliveryFeedRepository {
  _ItemsRepository(this.items);
  final List<DeliveryFeedItemEntity> items;

  @override
  Future<List<DeliveryFeedItemEntity>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  }) async =>
      items;
}

// 오류 발생 Fake
class _ErrorRepository implements DeliveryFeedRepository {
  @override
  Future<List<DeliveryFeedItemEntity>> fetchDeliveryFeed({
    required int page,
    required int size,
    ChannelTypeEnum? channelType,
  }) async =>
      throw Exception('Network error');
}

DeliveryFeedItemEntity _makeItem(int id) => DeliveryFeedItemEntity(
      deliveryLogId: id,
      notificationId: id,
      notificationTitle: 'Notification $id',
      channelId: 1,
      channelType: ChannelTypeEnum.slack,
      channelDisplayName: 'My Slack',
      deliveredContent: 'Content $id',
      deliveredAt: DateTime.now().subtract(Duration(minutes: id)),
      status: 'delivered',
    );

Future<void> _pumpScreen(
  WidgetTester tester, {
  required DeliveryFeedRepository repository,
  GoRouter? router,
}) async {
  final container = ProviderContainer(
    overrides: [
      deliveryFeedRepositoryProvider.overrideWith((ref) => repository),
    ],
  );
  addTearDown(container.dispose);

  if (router != null) {
    addTearDown(router.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(
          theme: AppTheme.darkTheme,
          routerConfig: router,
        ),
      ),
    );
  } else {
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const DeliveryFeedScreen(),
        ),
      ),
    );
  }
}

void main() {
  group('DeliveryFeedScreen', () {
    group('로딩 상태', () {
      testWidgets('shows CircularProgressIndicator while fetching',
          (tester) async {
        await _pumpScreen(tester, repository: _PendingRepository());
        await tester.pump(); // postFrameCallback 실행

        expect(find.byType(CircularProgressIndicator), findsOneWidget);
      });

      testWidgets('does not show items or empty state while loading',
          (tester) async {
        await _pumpScreen(tester, repository: _PendingRepository());
        await tester.pump();

        expect(find.byType(DeliveryBubble), findsNothing);
        expect(find.text('전달된 알림이 없습니다'), findsNothing);
      });
    });

    group('빈 상태', () {
      testWidgets('shows empty message when no items', (tester) async {
        await _pumpScreen(tester, repository: _EmptyRepository());
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.text('전달된 알림이 없습니다'), findsOneWidget);
      });

      testWidgets('shows 채널 관리로 이동 button in empty state', (tester) async {
        await _pumpScreen(tester, repository: _EmptyRepository());
        await tester.pump();
        await tester.pumpAndSettle();

        expect(
          find.widgetWithText(ElevatedButton, '채널 관리로 이동'),
          findsOneWidget,
        );
      });

      testWidgets('채널 관리로 이동 button navigates to /channels', (tester) async {
        final router = GoRouter(
          initialLocation: '/deliveries',
          routes: [
            GoRoute(
              path: '/deliveries',
              builder: (_, __) => const DeliveryFeedScreen(),
            ),
            GoRoute(
              path: '/channels',
              builder: (_, __) =>
                  const Scaffold(body: Text('Channels Screen')),
            ),
          ],
        );

        await _pumpScreen(
          tester,
          repository: _EmptyRepository(),
          router: router,
        );
        await tester.pump();
        await tester.pumpAndSettle();

        await tester.tap(find.widgetWithText(ElevatedButton, '채널 관리로 이동'));
        await tester.pumpAndSettle();

        expect(find.text('Channels Screen'), findsOneWidget);
      });

      testWidgets('shows send_outlined icon in empty state', (tester) async {
        await _pumpScreen(tester, repository: _EmptyRepository());
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.byIcon(Icons.send_outlined), findsOneWidget);
      });
    });

    group('아이템 목록 렌더링', () {
      testWidgets('renders DeliveryBubble for each item', (tester) async {
        final items = [_makeItem(1), _makeItem(2), _makeItem(3)];

        await _pumpScreen(tester, repository: _ItemsRepository(items));
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.byType(DeliveryBubble), findsNWidgets(3));
      });

      testWidgets('displays notification titles from items', (tester) async {
        final items = [_makeItem(1), _makeItem(2)];

        await _pumpScreen(tester, repository: _ItemsRepository(items));
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.text('Notification 1'), findsOneWidget);
        expect(find.text('Notification 2'), findsOneWidget);
      });

      testWidgets('shows ChannelFilterChips above the list', (tester) async {
        final items = [_makeItem(1)];

        await _pumpScreen(tester, repository: _ItemsRepository(items));
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.widgetWithText(FilterChip, 'All'), findsOneWidget);
        expect(find.widgetWithText(FilterChip, 'Slack'), findsOneWidget);
      });

      testWidgets('tapping bubble navigates to notification detail',
          (tester) async {
        final router = GoRouter(
          initialLocation: '/deliveries',
          routes: [
            GoRoute(
              path: '/deliveries',
              builder: (_, __) => const DeliveryFeedScreen(),
            ),
            GoRoute(
              path: '/notifications/:id',
              builder: (_, state) => Scaffold(
                body: Text('Notification ${state.pathParameters['id']}'),
              ),
            ),
          ],
        );

        await _pumpScreen(
          tester,
          repository: _ItemsRepository([_makeItem(99)]),
          router: router,
        );
        await tester.pump();
        await tester.pumpAndSettle();

        await tester.tap(find.byType(DeliveryBubble).first);
        await tester.pumpAndSettle();

        expect(find.text('Notification 99'), findsOneWidget);
      });
    });

    group('오류 상태', () {
      testWidgets('shows error message when load fails', (tester) async {
        await _pumpScreen(tester, repository: _ErrorRepository());
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.text('오류가 발생했습니다'), findsOneWidget);
      });

      testWidgets('shows retry button on error', (tester) async {
        await _pumpScreen(tester, repository: _ErrorRepository());
        await tester.pump();
        await tester.pumpAndSettle();

        expect(find.widgetWithText(ElevatedButton, '다시 시도'), findsOneWidget);
      });
    });
  });
}
