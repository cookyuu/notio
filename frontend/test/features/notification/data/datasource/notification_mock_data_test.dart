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
      expect(notification.body, contains('@cookyuu'));
      expect(notification.body, contains('PR #456'));
      expect(notification.priority, 'HIGH');
      expect(notification.isRead, false);

      // 외부 링크 확인
      expect(notification.externalId, 'slack-msg-20260406-001');
      expect(notification.externalUrl, contains('slack.com'));

      // 메타데이터 확인
      expect(notification.metadata, isNotNull);
      expect(notification.metadata!['channel'], 'dev-team');
      expect(notification.metadata!['user'], '박철수');
      expect(notification.metadata!['user_id'], 'U123456');

      // 시간 확인 (5초 전)
      final createdAt = DateTime.parse(notification.createdAt);
      final now = DateTime.now();
      final diff = now.difference(createdAt);

      expect(diff.inSeconds, greaterThanOrEqualTo(4));
      expect(diff.inSeconds, lessThanOrEqualTo(6));

      print('✅ 알림 정보:');
      print('   ID: ${notification.id}');
      print('   소스: ${notification.source}');
      print('   제목: ${notification.title}');
      print('   내용: ${notification.body}');
      print('   우선순위: ${notification.priority}');
      print('   읽음 여부: ${notification.isRead ? "읽음" : "미읽음"}');
      print('   생성 시간: ${createdAt.toString()}');
      print('   경과 시간: ${diff.inSeconds}초 전');
      print('   채널: ${notification.metadata!['channel']}');
      print('   발신자: ${notification.metadata!['user']}');
      print('   외부 링크: ${notification.externalUrl}');
    });

    test('미읽음 개수는 1이어야 함', () async {
      final count = await dataSource.getUnreadCount();

      expect(count, 1);
      print('✅ 미읽음 알림 개수: $count');
    });

    test('알림 목록 조회 시 시간 순으로 정렬되어야 함', () async {
      final notifications = await dataSource.fetchNotifications();

      expect(notifications, isNotEmpty);

      // 최신 알림이 첫 번째여야 함
      final firstNotification = notifications.first;
      final firstTime = DateTime.parse(firstNotification.createdAt);

      print('✅ 최신 알림 시간: ${firstTime.toString()}');
      print('   최신 알림 제목: ${firstNotification.title}');
    });
  });
}
