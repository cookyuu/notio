import 'package:notio_app/features/analytics/data/datasource/analytics_remote_datasource.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';
import 'package:notio_app/features/analytics/domain/repository/analytics_repository.dart';

/// Implementation of AnalyticsRepository
class AnalyticsRepositoryImpl implements AnalyticsRepository {
  final AnalyticsRemoteDataSource _remoteDataSource;

  // In-memory cache
  WeeklyAnalyticsEntity? _cachedSummary;
  DateTime? _cacheTime;

  // Cache TTL: 1 hour
  static const _cacheDuration = Duration(hours: 1);

  AnalyticsRepositoryImpl({
    required AnalyticsRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<WeeklyAnalyticsEntity> fetchWeeklySummary() async {
    // Check if cache is valid
    if (_cachedSummary != null && _cacheTime != null) {
      final now = DateTime.now();
      if (now.difference(_cacheTime!) < _cacheDuration) {
        // Return cached data
        return _cachedSummary!;
      }
    }

    try {
      // Fetch from remote
      final model = await _remoteDataSource.fetchWeeklySummary();
      final entity = model.toEntity();

      // Update cache
      _cachedSummary = entity;
      _cacheTime = DateTime.now();

      return entity;
    } catch (e) {
      // If fetch fails and we have cached data, return it
      if (_cachedSummary != null) {
        return _cachedSummary!;
      }
      rethrow;
    }
  }

  /// Clear cache (useful for testing or manual refresh)
  void clearCache() {
    _cachedSummary = null;
    _cacheTime = null;
  }
}
