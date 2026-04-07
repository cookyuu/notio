import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/analytics/data/datasource/analytics_remote_datasource.dart';
import 'package:notio_app/features/analytics/data/repository/analytics_repository_impl.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';
import 'package:notio_app/features/analytics/domain/repository/analytics_repository.dart';

/// Provider for analytics remote data source
final analyticsRemoteDataSourceProvider = Provider<AnalyticsRemoteDataSource>((ref) {
  return AnalyticsRemoteDataSource();
});

/// Provider for analytics repository
final analyticsRepositoryProvider = Provider<AnalyticsRepository>((ref) {
  final remoteDataSource = ref.watch(analyticsRemoteDataSourceProvider);
  return AnalyticsRepositoryImpl(remoteDataSource: remoteDataSource);
});

/// Provider for weekly analytics
///
/// Automatically fetches and caches weekly analytics data
/// Cache is managed by the repository (1 hour TTL)
final weeklyAnalyticsProvider = FutureProvider<WeeklyAnalyticsEntity>((ref) async {
  final repository = ref.watch(analyticsRepositoryProvider);
  return repository.fetchWeeklySummary();
});

/// Provider to manually refresh analytics
///
/// This invalidates the cache and refetches data
final refreshAnalyticsProvider = Provider<void Function()>((ref) {
  return () {
    // Invalidate the future provider to trigger a refetch
    ref.invalidate(weeklyAnalyticsProvider);
  };
});
