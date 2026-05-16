import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:notio_app/features/analytics/data/model/ai_usage_model.dart';
import 'package:notio_app/features/analytics/data/model/weekly_analytics_model.dart';
import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';

/// Remote data source for analytics
class AnalyticsRemoteDataSource {
  final Dio _dio;

  AnalyticsRemoteDataSource(this._dio);

  /// Fetch weekly summary analytics
  Future<WeeklyAnalyticsModel> fetchWeeklySummary() async {
    try {
      final response = await _dio.get('/api/v1/analytics/weekly');

      if (response.data['success'] == true) {
        return WeeklyAnalyticsModel.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Fetch AI token usage analytics
  Future<AiUsageModel> fetchAiUsage(AiUsageFilter filter) async {
    final fmt = DateFormat('yyyy-MM-dd');
    try {
      final response = await _dio.get(
        '/api/v1/analytics/ai-usage',
        queryParameters: {
          'granularity': filter.granularity.name.toUpperCase(),
          'startDate': fmt.format(filter.startDate),
          'endDate': fmt.format(filter.endDate),
        },
      );

      if (response.data['success'] == true) {
        final data = response.data['data'];
        if (data == null) throw Exception('응답 데이터가 없습니다.');
        return AiUsageModel.fromJson(data as Map<String, dynamic>);
      } else {
        final error = response.data['error'];
        throw Exception(error != null ? error['message'] : '알 수 없는 오류');
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    } catch (e, st) {
      // ignore: avoid_print
      print('[AiUsage] parse error: $e\n$st');
      rethrow;
    }
  }
}
