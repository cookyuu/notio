import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/network/network_status_provider.dart';
import 'package:notio_app/core/network/sync_service.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/features/notification/domain/entity/notification_summary_entity.dart';
import 'package:notio_app/features/notification/domain/repository/notification_repository.dart';
import 'package:notio_app/features/notification/presentation/providers/notification_providers.dart';
import 'package:notio_app/features/notification/presentation/screens/notifications_screen.dart';

class _FakeNotificationRepository implements NotificationRepository {
  _FakeNotificationRepository({
    required this.summaries,
    this.detail,
    this.detailError,
    this.unreadCount = 0,
  });

  final List<NotificationSummaryEntity> summaries;
  final NotificationDetailEntity? detail;
  final Object? detailError;
  final int unreadCount;
  int fetchDetailCallCount = 0;

  @override
  Future<List<NotificationSummaryEntity>> fetchNotifications({
    NotificationSource? source,
    int page = 0,
    int size = 20,
  }) async {
    return summaries;
  }

  @override
  Future<NotificationDetailEntity> getNotificationDetail(int id) async {
    fetchDetailCallCount++;
    if (detailError != null) {
      throw detailError!;
    }
    if (detail == null) {
      throw Exception('Missing detail');
    }
    return detail!;
  }

  @override
  Future<int> getUnreadCount() async => unreadCount;

  @override
  Future<void> markAsRead(int notificationId) async {}

  @override
  Future<void> markAllAsRead() async {}

  @override
  Future<NotificationEntity?> getNotificationById(int id) async => null;

  @override
  Future<void> deleteNotification(int id) async {}

  @override
  Future<List<NotificationEntity>> getCachedNotifications({
    NotificationSource? source,
  }) async {
    return const [];
  }

  @override
  Future<void> cacheNotifications(List<NotificationEntity> notifications) async {}

  @override
  Future<void> clearCache() async {}
}

void main() {
  group('NotificationsScreen', () {
    final summary = NotificationSummaryEntity(
      id: 1,
      source: NotificationSource.github,
      title: 'Pull request review',
      bodyPreview: 'A reviewer left a comment',
      priority: NotificationPriority.high,
      isRead: false,
      createdAt: DateTime(2026, 4, 17),
    );

    final detail = NotificationDetailEntity(
      id: 1,
      source: NotificationSource.github,
      title: 'Pull request review',
      body: 'A reviewer left a detailed comment on your pull request.',
      priority: NotificationPriority.high,
      isRead: true,
      createdAt: DateTime(2026, 4, 17),
      updatedAt: DateTime(2026, 4, 17),
      externalUrl: 'https://github.com/notio/pull/1',
      metadata: const {'repository': 'notio'},
    );

    Future<_FakeNotificationRepository> pumpScreen(
      WidgetTester tester, {
      NotificationDetailEntity? notificationDetail,
      Object? detailError,
    }) async {
      final repository = _FakeNotificationRepository(
        summaries: [summary],
        detail: notificationDetail,
        detailError: detailError,
        unreadCount: 1,
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            notificationRepositoryProvider.overrideWithValue(repository),
            networkStatusProvider.overrideWith((ref) => NetworkStatus.online),
            syncServiceProvider.overrideWith(
              (ref) => SyncService(
                repository,
                ref,
              ),
            ),
          ],
          child: const MaterialApp(
            home: NotificationsScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      return repository;
    }

    testWidgets('opens detail modal after tapping a notification', (tester) async {
      final repository = await pumpScreen(
        tester,
        notificationDetail: detail,
      );

      await tester.tap(find.text('Pull request review'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));

      expect(repository.fetchDetailCallCount, 1);
      expect(
        find.text('A reviewer left a detailed comment on your pull request.'),
        findsOneWidget,
      );
      expect(find.text('외부 링크'), findsOneWidget);
    });

    testWidgets('shows snackbar and does not open modal when detail fetch fails',
        (tester) async {
      final repository = await pumpScreen(
        tester,
        detailError: Exception('detail failed'),
      );

      await tester.tap(find.text('Pull request review'));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(repository.fetchDetailCallCount, 1);
      expect(find.textContaining('상세 알림을 불러오지 못했습니다.'), findsOneWidget);
      expect(
        find.text('A reviewer left a detailed comment on your pull request.'),
        findsNothing,
      );
      expect(find.text('외부 링크'), findsNothing);
    });
  });
}
