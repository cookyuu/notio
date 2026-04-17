import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';

void main() {
  group('Mock Data - Single Notification', () {
    test('방금 도착한 알림 1개가 있어야 함', () async {
      final dataSource = NotificationRemoteDataSource(
        _createDio((options) {
          if (options.path == '/api/v1/notifications') {
            return {
              'success': true,
              'data': {
                'content': [
                  {
                    'id': 1,
                    'source': 'SLACK',
                    'title': '#dev-team 채널에 멘션',
                    'body_preview': '빌드 실패 원인 확인 부탁드립니다.',
                    'priority': 'HIGH',
                    'is_read': false,
                    'created_at': '2026-04-17T10:00:00Z',
                  },
                ],
              },
              'error': null,
            };
          }
          throw UnimplementedError('Unhandled path: ${options.path}');
        }),
      );

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
      expect(notification.createdAt, '2026-04-17T10:00:00Z');
    });

    test('미읽음 개수는 1이어야 함', () async {
      final dataSource = NotificationRemoteDataSource(
        _createDio((options) {
          if (options.path == '/api/v1/notifications/unread-count') {
            return {
              'success': true,
              'data': {
                'count': 1,
              },
              'error': null,
            };
          }
          throw UnimplementedError('Unhandled path: ${options.path}');
        }),
      );

      final count = await dataSource.getUnreadCount();

      expect(count, 1);
    });

    test('알림 목록 조회 시 시간 순으로 정렬되어야 함', () async {
      final dataSource = NotificationRemoteDataSource(
        _createDio((options) {
          if (options.path == '/api/v1/notifications') {
            return {
              'success': true,
              'data': {
                'content': [
                  {
                    'id': 2,
                    'source': 'GITHUB',
                    'title': 'Older notification',
                    'body_preview': 'Earlier event',
                    'priority': 'LOW',
                    'is_read': true,
                    'created_at': '2026-04-17T09:00:00Z',
                  },
                  {
                    'id': 1,
                    'source': 'SLACK',
                    'title': 'Latest notification',
                    'body_preview': 'Most recent event',
                    'priority': 'HIGH',
                    'is_read': false,
                    'created_at': '2026-04-17T10:00:00Z',
                  },
                ],
              },
              'error': null,
            };
          }
          throw UnimplementedError('Unhandled path: ${options.path}');
        }),
      );

      final notifications = await dataSource.fetchNotifications();

      expect(notifications, hasLength(2));
      expect(notifications.first.id, 2);
      expect(notifications.last.id, 1);
    });
  });
}

Dio _createDio(Map<String, dynamic> Function(RequestOptions options) responder) {
  final dio = Dio();
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        handler.resolve(
          Response<Map<String, dynamic>>(
            requestOptions: options,
            data: responder(options),
            statusCode: 200,
          ),
        );
      },
    ),
  );
  return dio;
}
