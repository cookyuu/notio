import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/data/datasource/notification_local_datasource.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';
import 'package:notio_app/features/notification/data/repository/notification_repository_impl.dart';

void main() {
  late NotificationRepositoryImpl repository;
  late NotificationRemoteDataSource remoteDataSource;
  late NotificationLocalDataSource localDataSource;
  late Dio dio;

  setUp(() {
    dio = Dio();
    remoteDataSource = NotificationRemoteDataSource(dio);
    localDataSource = NotificationLocalDataSource();
    repository = NotificationRepositoryImpl(
      remoteDataSource: remoteDataSource,
      localDataSource: localDataSource,
    );
  });

  group('NotificationRepository', () {
    test('fetchNotifications should return mock data', () async {
      final notifications = await repository.fetchNotifications();

      expect(notifications, isNotEmpty);
      expect(notifications.length, 1);
      expect(notifications.first.id, 1);
      expect(notifications.first.source, NotificationSource.slack);
    });

    test('fetchNotifications with source filter should return filtered data',
        () async {
      final slackNotifications = await repository.fetchNotifications(
        source: NotificationSource.slack,
      );

      expect(slackNotifications.length, 1);
      for (final notification in slackNotifications) {
        expect(notification.source, NotificationSource.slack);
      }

      // GitHub 알림은 없어야 함
      final githubNotifications = await repository.fetchNotifications(
        source: NotificationSource.github,
      );
      expect(githubNotifications.length, 0);
    });

    test('getUnreadCount should return correct count', () async {
      final count = await repository.getUnreadCount();

      expect(count, 1);
    });

    test('markAsRead should update notification', () async {
      // First fetch to cache
      await repository.fetchNotifications();

      // Mark as read
      await repository.markAsRead(1);

      // Verify it's read in cache
      final notification = await repository.getNotificationById(1);
      expect(notification?.isRead, true);
    });

    test('getCachedNotifications should return cached data', () async {
      // First fetch to populate cache
      await repository.fetchNotifications();

      // Get from cache
      final cachedNotifications = await repository.getCachedNotifications();

      expect(cachedNotifications, isNotEmpty);
    });

    test('getCachedNotifications with source filter', () async {
      // First fetch to populate cache
      await repository.fetchNotifications();

      // Get Slack notifications from cache
      final slackNotifications = await repository.getCachedNotifications(
        source: NotificationSource.slack,
      );

      expect(slackNotifications.length, 1);
      for (final notification in slackNotifications) {
        expect(notification.source, NotificationSource.slack);
      }

      // GitHub notifications should be empty
      final githubNotifications = await repository.getCachedNotifications(
        source: NotificationSource.github,
      );
      expect(githubNotifications.length, 0);
    });
  });
}
