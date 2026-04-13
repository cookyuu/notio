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
      final response = await _dio.get(
        '/api/v1/notifications',
        queryParameters: {
          if (source != null) 'source': source,
          'page': page,
          'size': size,
        },
      );

      if (response.data['success'] == true) {
        final data = response.data['data'];
        final items = data is List<dynamic>
            ? data
            : (data['content'] as List<dynamic>? ?? const []);
        return items.map((json) => NotificationModel.fromJson(json)).toList();
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
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
