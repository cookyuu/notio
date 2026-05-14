import 'package:dio/dio.dart';
import '../model/notification_channel_model.dart';
import '../model/routing_rule_model.dart';

class ChannelRemoteDataSource {
  final Dio _dio;

  ChannelRemoteDataSource(this._dio);

  Future<List<NotificationChannelModel>> fetchChannels() async {
    try {
      final response = await _dio.get('/api/v1/channels');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      final List<dynamic> data = response.data['data'];
      return data
          .map((json) =>
              NotificationChannelModel.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<NotificationChannelModel> createChannel(
    Map<String, dynamic> data,
  ) async {
    try {
      final response = await _dio.post('/api/v1/channels', data: data);
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      return NotificationChannelModel.fromJson(
          response.data['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<NotificationChannelModel> updateChannel(
    int id,
    Map<String, dynamic> data,
  ) async {
    try {
      final response = await _dio.put('/api/v1/channels/$id', data: data);
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      return NotificationChannelModel.fromJson(
          response.data['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<void> deleteChannel(int id) async {
    try {
      final response = await _dio.delete('/api/v1/channels/$id');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<void> pauseChannel(int id) async {
    try {
      final response = await _dio.patch('/api/v1/channels/$id/pause');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<void> resumeChannel(int id) async {
    try {
      final response = await _dio.patch('/api/v1/channels/$id/resume');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<void> testChannel(int id) async {
    try {
      final response = await _dio.post('/api/v1/channels/$id/test');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      final deliveryData = response.data['data'] as Map<String, dynamic>?;
      if (deliveryData != null && deliveryData['success'] != true) {
        throw Exception(deliveryData['errorMessage'] ?? '채널 전송 실패');
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<List<RoutingRuleModel>> fetchRoutingRules() async {
    try {
      final response = await _dio.get('/api/v1/routing-rules');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      final List<dynamic> data = response.data['data'];
      return data
          .map((json) =>
              RoutingRuleModel.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<RoutingRuleModel> createRoutingRule(Map<String, dynamic> data) async {
    try {
      final response = await _dio.post('/api/v1/routing-rules', data: data);
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      return RoutingRuleModel.fromJson(
          response.data['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<RoutingRuleModel> updateRoutingRule(
    int id,
    Map<String, dynamic> data,
  ) async {
    try {
      final response = await _dio.put('/api/v1/routing-rules/$id', data: data);
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
      return RoutingRuleModel.fromJson(
          response.data['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<void> deleteRoutingRule(int id) async {
    try {
      final response = await _dio.delete('/api/v1/routing-rules/$id');
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  Future<void> reorderRoutingRules(List<int> orderedIds) async {
    try {
      final response = await _dio.patch(
        '/api/v1/routing-rules/reorder',
        data: {'ordered_ids': orderedIds},
      );
      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }
}
