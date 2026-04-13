import 'package:dio/dio.dart';
import 'package:notio_app/features/notification/data/model/notification_model.dart';

/// Remote data source for notifications
class NotificationRemoteDataSource {
  final Dio _dio;

  NotificationRemoteDataSource(this._dio);

  /// Fetch notifications from API
  Future<List<NotificationModel>> fetchNotifications({
    String? source,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _requestNotifications(
        source: source,
        page: page,
        size: size,
      );
      return _parseNotifications(response.data);
    } on DioException catch (e) {
      if (_shouldRetryWithoutSource(e, source)) {
        final fallbackResponse = await _requestNotifications(
          page: page,
          size: size,
        );
        return _parseNotifications(fallbackResponse.data)
            .where((notification) => notification.source == source)
            .toList();
      }
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<Response<dynamic>> _requestNotifications({
    required int page,
    required int size,
    String? source,
  }) {
    return _dio.get(
      '/api/v1/notifications',
      queryParameters: {
        if (source != null) 'source': source,
        'page': page,
        'size': size,
      },
    );
  }

  List<NotificationModel> _parseNotifications(dynamic responseData) {
    if (responseData['success'] != true) {
      throw Exception(responseData['error']['message']);
    }

    final data = responseData['data'];
    final items = switch (data) {
      List<dynamic>() => data,
      Map<String, dynamic>() => (data['content'] as List<dynamic>?) ?? const [],
      _ => const <dynamic>[],
    };
    return items.map((json) => NotificationModel.fromJson(json)).toList();
  }

  bool _shouldRetryWithoutSource(DioException exception, String? source) {
    if (source == null || exception.response?.statusCode != 400) {
      return false;
    }

    final responseData = exception.response?.data;
    if (responseData is! Map<String, dynamic>) {
      return false;
    }

    final error = responseData['error'];
    if (error is! Map<String, dynamic>) {
      return false;
    }

    return error['code'] == 'INVALID_REQUEST';
  }

  /// Get unread notification count from API
  Future<int> getUnreadCount() async {
    try {
      final response = await _dio.get('/api/v1/notifications/unread-count');

      if (response.data['success'] == true) {
        return response.data['data']['count'];
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Mark notification as read
  Future<void> markAsRead(int notificationId) async {
    try {
      final response = await _dio.patch(
        '/api/v1/notifications/$notificationId/read',
      );

      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Mark all notifications as read
  Future<void> markAllAsRead() async {
    try {
      final response = await _dio.patch('/api/v1/notifications/read-all');

      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Delete notification from API
  Future<void> deleteNotification(int notificationId) async {
    try {
      final response = await _dio.delete('/api/v1/notifications/$notificationId');

      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }
}
