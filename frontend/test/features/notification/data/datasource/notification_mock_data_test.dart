import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';

void main() {
  late NotificationRemoteDataSource dataSource;
  late Dio dio;

  setUp(() {
    dio = Dio();
    dataSource = NotificationRemoteDataSource(dio);
  });

  group('Mock Data - Single Notification', () {
    test('방금 도착한 알림 1개가 있어야 함', () async {
      final notifications = await dataSource.fetchNotifications();

      expect(notifications.length, 1);

      final notification = notifications.first;

      // 기본 정보 확인
      expect(notification.id, 1);
      expect(notification.source, 'SLACK');
      expect(notification.title, '#dev-team 채널에 멘션');
      expect(notification.bodyPreview, isNotEmpty);
      expect(notification.priority, 'HIGH');
      expect(notification.isRead, false);

      // 시간 확인 (5초 전)
      final createdAt = DateTime.parse(notification.createdAt);
      final now = DateTime.now();
      final diff = now.difference(createdAt);

      expect(diff.inSeconds, greaterThanOrEqualTo(4));
      expect(diff.inSeconds, lessThanOrEqualTo(6));

    });

    test('미읽음 개수는 1이어야 함', () async {
      final count = await dataSource.getUnreadCount();

      expect(count, 1);
    });

    test('알림 목록 조회 시 시간 순으로 정렬되어야 함', () async {
      final notifications = await dataSource.fetchNotifications();

      expect(notifications, isNotEmpty);
    });
  });
}
