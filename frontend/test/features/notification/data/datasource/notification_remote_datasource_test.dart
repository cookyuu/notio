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
