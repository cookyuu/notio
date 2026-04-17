import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';
import 'package:notio_app/features/notification/domain/entity/notification_summary_entity.dart';
import 'package:notio_app/features/notification/domain/repository/notification_repository.dart';
import 'package:notio_app/features/notification/presentation/providers/notification_providers.dart';
import 'package:notio_app/features/notification/presentation/providers/notifications_notifier.dart';

class _FakeNotificationRepository implements NotificationRepository {
  List<NotificationSummaryEntity> summaries = const [];
  NotificationDetailEntity? detail;
  Object? detailError;
  int unreadCount = 0;
  int fetchDetailCallCount = 0;
  int unreadCountCallCount = 0;

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
  Future<int> getUnreadCount() async {
    unreadCountCallCount++;
    return unreadCount;
  }

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
  Future<void> cacheNotifications(
      List<NotificationEntity> notifications) async {}

  @override
  Future<void> clearCache() async {}
}

void main() {
  late ProviderContainer container;
  late _FakeNotificationRepository repository;

  setUp(() {
    repository = _FakeNotificationRepository()
      ..summaries = [
        NotificationSummaryEntity(
          id: 1,
          source: NotificationSource.github,
          title: 'Pull request review',
          bodyPreview: 'A reviewer left a comment',
          priority: NotificationPriority.high,
          isRead: false,
          createdAt: DateTime(2026, 4, 17),
        ),
      ]
      ..detail = NotificationDetailEntity(
        id: 1,
        source: NotificationSource.github,
        title: 'Pull request review',
        body: 'A reviewer left a detailed comment on your pull request.',
        priority: NotificationPriority.high,
        isRead: true,
        createdAt: DateTime(2026, 4, 17),
        updatedAt: DateTime(2026, 4, 17),
        externalUrl: 'https://github.com/notio/pull/1',
        metadata: const {'repository': 'notio', 'reviewer': 'alice'},
      )
      ..unreadCount = 1;

    container = ProviderContainer(
      overrides: [
        notificationRepositoryProvider.overrideWithValue(repository),
      ],
    );
  });

  tearDown(() {
    container.dispose();
  });

  group('NotificationsNotifier', () {
    test('fetchNotificationDetail calls detail API once', () async {
      await container.read(notificationsProvider.notifier).fetchNotifications(
            refresh: true,
          );

      final result = await container
          .read(notificationsProvider.notifier)
          .fetchNotificationDetail(1);

      expect(repository.fetchDetailCallCount, 1);
      expect(result.id, 1);
    });

    test(
        'fetchNotificationDetail updates item read state and invalidates unread count',
        () async {
      container.listen<AsyncValue<int>>(
        unreadCountProvider,
        (_, __) {},
        fireImmediately: true,
      );
      await container.read(unreadCountProvider.future);
      expect(repository.unreadCountCallCount, 1);

      await container.read(notificationsProvider.notifier).fetchNotifications(
            refresh: true,
          );

      await container
          .read(notificationsProvider.notifier)
          .fetchNotificationDetail(1);

      final state = container.read(notificationsProvider);
      expect(state.notifications.single.isRead, isTrue);

      repository.unreadCount = 0;
      await container.read(unreadCountProvider.future);
      expect(repository.unreadCountCallCount, 2);
    });

    test('fetchNotificationDetail keeps state unchanged when request fails',
        () async {
      repository.detailError = Exception('detail failed');

      await container.read(notificationsProvider.notifier).fetchNotifications(
            refresh: true,
          );

      await expectLater(
        container
            .read(notificationsProvider.notifier)
            .fetchNotificationDetail(1),
        throwsException,
      );

      final state = container.read(notificationsProvider);
      expect(state.notifications.single.isRead, isFalse);
    });

    test('refresh synchronizes list read state with latest server response',
        () async {
      await container.read(notificationsProvider.notifier).fetchNotifications(
            refresh: true,
          );

      expect(
        container.read(notificationsProvider).notifications.single.isRead,
        isFalse,
      );

      repository.summaries = [
        NotificationSummaryEntity(
          id: 1,
          source: NotificationSource.github,
          title: 'Pull request review',
          bodyPreview: 'A reviewer left a comment',
          priority: NotificationPriority.high,
          isRead: true,
          createdAt: DateTime(2026, 4, 17),
        ),
      ];

      await container.read(notificationsProvider.notifier).fetchNotifications(
            refresh: true,
          );

      expect(
        container.read(notificationsProvider).notifications.single.isRead,
        isTrue,
      );
    });
  });
}
