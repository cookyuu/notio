import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/database/database_providers.dart';
import 'package:notio_app/core/network/dio_client.dart';
import 'package:notio_app/features/notification/data/datasource/notification_local_datasource.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';
import 'package:notio_app/features/notification/data/repository/notification_repository_impl.dart';
import 'package:notio_app/features/notification/domain/repository/notification_repository.dart';
import 'package:notio_app/shared/constant/api_constants.dart';

/// Dio Provider
final dioProvider = Provider<Dio>((ref) {
  return DioClient.create(
    baseUrl: ApiConstants.baseUrl,
    enableLogging: true,
  );
});

/// Remote DataSource Provider
final notificationRemoteDataSourceProvider =
    Provider<NotificationRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return NotificationRemoteDataSource(dio);
});

/// Local DataSource Provider (Drift-based)
final notificationLocalDataSourceProvider =
    Provider<NotificationLocalDataSource>((ref) {
  final database = ref.watch(appDatabaseProvider);
  return NotificationLocalDataSource(database);
});

/// Repository Provider
final notificationRepositoryProvider = Provider<NotificationRepository>((ref) {
  final remoteDataSource = ref.watch(notificationRemoteDataSourceProvider);
  final localDataSource = ref.watch(notificationLocalDataSourceProvider);

  return NotificationRepositoryImpl(
    remoteDataSource: remoteDataSource,
    localDataSource: localDataSource,
  );
});
