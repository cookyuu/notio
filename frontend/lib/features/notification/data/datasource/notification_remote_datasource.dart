import 'package:dio/dio.dart';
import 'package:notio_app/features/notification/data/model/notification_model.dart';

/// Remote data source for notifications
class NotificationRemoteDataSource {
  // final Dio _dio;  // TODO: Uncomment when backend API is ready

  NotificationRemoteDataSource(Dio dio);

  /// Fetch notifications from API
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<List<NotificationModel>> fetchNotifications({
    String? source,
    int page = 0,
    int size = 20,
  }) async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.get(
    //     '/api/v1/notifications',
    //     queryParameters: {
    //       if (source != null) 'source': source,
    //       'page': page,
    //       'size': size,
    //     },
    //   );
    //
    //   if (response.data['success'] == true) {
    //     final List<dynamic> data = response.data['data'];
    //     return data.map((json) => NotificationModel.fromJson(json)).toList();
    //   } else {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock data for development
    await Future.delayed(const Duration(milliseconds: 800));
    return _getMockNotifications(source: source, page: page, size: size);
  }

  /// Get unread notification count from API
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<int> getUnreadCount() async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.get('/api/v1/notifications/unread-count');
    //
    //   if (response.data['success'] == true) {
    //     return response.data['data']['count'];
    //   } else {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock data - 1개의 미읽음 알림
    await Future.delayed(const Duration(milliseconds: 300));
    return 1;
  }

  /// Mark notification as read
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<void> markAsRead(int notificationId) async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.patch(
    //     '/api/v1/notifications/$notificationId/read',
    //   );
    //
    //   if (response.data['success'] != true) {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock implementation
    await Future.delayed(const Duration(milliseconds: 200));
  }

  /// Mark all notifications as read
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<void> markAllAsRead() async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.patch('/api/v1/notifications/read-all');
    //
    //   if (response.data['success'] != true) {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock implementation
    await Future.delayed(const Duration(milliseconds: 300));
  }

  /// Get mock notifications for development
  List<NotificationModel> _getMockNotifications({
    String? source,
    int page = 0,
    int size = 20,
  }) {
    final allMockData = [
      // 방금 도착한 알림 (지금부터 5초 전)
      NotificationModel(
        id: 1,
        source: 'SLACK',
        title: '#dev-team 채널에 멘션',
        body: '@cookyuu 안녕하세요! PR #456 리뷰 부탁드립니다. 알림 기능 구현 잘 되어가시나요? 👀',
        priority: 'HIGH',
        isRead: false,
        createdAt: DateTime.now().subtract(const Duration(seconds: 5)).toIso8601String(),
        externalId: 'slack-msg-20260406-001',
        externalUrl: 'https://notio-team.slack.com/archives/C123456/p1712421234',
        metadata: {
          'channel': 'dev-team',
          'user': '박철수',
          'user_id': 'U123456',
          'has_attachments': false,
          'thread_ts': '1712421234.123456',
        },
      ),
    ];

    // Filter by source if specified
    final filteredData = source != null
        ? allMockData.where((n) => n.source == source).toList()
        : allMockData;

    // Apply pagination
    final startIndex = page * size;
    if (startIndex >= filteredData.length) {
      return [];
    }

    final endIndex = (startIndex + size).clamp(0, filteredData.length);
    return filteredData.sublist(startIndex, endIndex);
  }
}
