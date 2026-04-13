import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';

void main() {
  group('NotificationRemoteDataSource', () {
    test('fetchNotifications returns empty list when data is null', () async {
      final dataSource = NotificationRemoteDataSource(
        _createDioWithResponse({
          'success': true,
          'data': null,
          'error': null,
        }),
      );

      final notifications = await dataSource.fetchNotifications();

      expect(notifications, isEmpty);
    });

    test('fetchNotifications returns empty list when content is missing', () async {
      final dataSource = NotificationRemoteDataSource(
        _createDioWithResponse({
          'success': true,
          'data': <String, dynamic>{},
          'error': null,
        }),
      );

      final notifications = await dataSource.fetchNotifications();

      expect(notifications, isEmpty);
    });

    test('fetchNotifications retries without source on invalid request', () async {
      final dio = Dio();
      var callCount = 0;

      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            callCount++;

            if (callCount == 1) {
              handler.reject(
                DioException(
                  requestOptions: options,
                  response: Response<Map<String, dynamic>>(
                    requestOptions: options,
                    statusCode: 400,
                    data: {
                      'success': false,
                      'error': {
                        'code': 'INVALID_REQUEST',
                        'message': '잘못된 요청입니다.',
                      },
                    },
                  ),
                ),
              );
              return;
            }

            expect(options.queryParameters.containsKey('source'), isFalse);

            handler.resolve(
              Response<Map<String, dynamic>>(
                requestOptions: options,
                statusCode: 200,
                data: {
                  'success': true,
                  'data': {
                    'content': [
                      {
                        'id': 1,
                        'source': 'CLAUDE',
                        'title': 'Claude alert',
                        'body': 'Review completed',
                        'priority': 'MEDIUM',
                        'is_read': false,
                        'created_at': '2026-04-13T10:00:00Z',
                        'external_id': null,
                        'external_url': null,
                        'metadata': {},
                      },
                      {
                        'id': 2,
                        'source': 'SLACK',
                        'title': 'Slack alert',
                        'body': 'Mention received',
                        'priority': 'HIGH',
                        'is_read': false,
                        'created_at': '2026-04-13T09:00:00Z',
                        'external_id': null,
                        'external_url': null,
                        'metadata': {},
                      },
                    ],
                  },
                  'error': null,
                },
              ),
            );
          },
        ),
      );

      final dataSource = NotificationRemoteDataSource(dio);
      final notifications = await dataSource.fetchNotifications(source: 'CLAUDE');

      expect(callCount, 2);
      expect(notifications, hasLength(1));
      expect(notifications.first.source, 'CLAUDE');
    });
  });
}

Dio _createDioWithResponse(Map<String, dynamic> responseData) {
  final dio = Dio();
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        handler.resolve(
          Response<Map<String, dynamic>>(
            requestOptions: options,
            data: responseData,
            statusCode: 200,
          ),
        );
      },
    ),
  );
  return dio;
}
