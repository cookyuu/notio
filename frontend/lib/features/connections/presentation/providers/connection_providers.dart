import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/network/dio_client.dart';
import 'package:notio_app/shared/constant/api_constants.dart';
import '../../data/datasource/connection_remote_datasource.dart';
import '../../data/repository/connection_repository_impl.dart';
import '../../domain/repository/connection_repository.dart';

/// Dio Provider (shared across features)
final dioProvider = Provider<Dio>((ref) {
  return DioClient.create(
    baseUrl: ApiConstants.baseUrl,
    enableLogging: true,
  );
});

/// Provider for connection remote data source
final connectionRemoteDataSourceProvider =
    Provider<ConnectionRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return ConnectionRemoteDataSource(dio);
});

/// Provider for connection repository
///
/// IMPORTANT: This repository does NOT cache API keys or sensitive credentials
/// API keys are only returned in create/rotate responses and should be displayed once
final connectionRepositoryProvider = Provider<ConnectionRepository>((ref) {
  final remoteDataSource = ref.watch(connectionRemoteDataSourceProvider);
  return ConnectionRepositoryImpl(remoteDataSource: remoteDataSource);
});
