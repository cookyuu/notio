import 'package:dio/dio.dart';
import 'package:notio_app/features/analytics/data/model/weekly_analytics_model.dart';

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
}
