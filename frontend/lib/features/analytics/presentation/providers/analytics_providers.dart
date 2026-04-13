import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/network/dio_client.dart';
import 'package:notio_app/features/analytics/data/datasource/analytics_remote_datasource.dart';
import 'package:notio_app/features/analytics/data/repository/analytics_repository_impl.dart';
import 'package:notio_app/features/analytics/domain/entity/weekly_analytics_entity.dart';
import 'package:notio_app/features/analytics/domain/repository/analytics_repository.dart';
import 'package:notio_app/shared/constant/api_constants.dart';

/// Dio Provider
final dioProvider = Provider<Dio>((ref) {
  return DioClient.create(
    baseUrl: ApiConstants.baseUrl,
    enableLogging: true,
  );
});

/// Provider for analytics remote data source
final analyticsRemoteDataSourceProvider = Provider<AnalyticsRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return AnalyticsRemoteDataSource(dio);
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
    // Recreate the repository first so its in-memory cache is dropped.
    ref.invalidate(analyticsRepositoryProvider);

    // Invalidate the future provider to trigger a refetch
    ref.invalidate(weeklyAnalyticsProvider);
  };
});
