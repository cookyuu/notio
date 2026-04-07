import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/database/database_providers.dart';
import 'package:notio_app/core/network/dio_client.dart';
import 'package:notio_app/features/chat/data/datasources/chat_local_datasource.dart';
import 'package:notio_app/features/chat/data/datasources/chat_remote_datasource.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';
import 'package:notio_app/features/chat/data/repositories/chat_repository_impl.dart';
import 'package:notio_app/features/chat/domain/repositories/chat_repository.dart';
import 'package:notio_app/features/chat/presentation/providers/chat_notifier.dart';
import 'package:notio_app/features/chat/presentation/providers/chat_state.dart';

/// Dio provider (reuse from core)
final dioProvider = Provider<Dio>((ref) {
  // TODO: Replace with proper base URL from environment
  return DioClient.create(
    baseUrl: 'http://localhost:8080',
    enableLogging: true,
  );
});

/// Chat local data source provider (Drift-based)
final chatLocalDataSourceProvider = Provider<ChatLocalDataSource>((ref) {
  final database = ref.watch(appDatabaseProvider);
  return ChatLocalDataSource(database);
});

/// Chat remote data source provider
final chatRemoteDataSourceProvider = Provider<ChatRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return ChatRemoteDataSource(dio);
});

/// Chat repository provider
final chatRepositoryProvider = Provider<ChatRepository>((ref) {
  final remoteDataSource = ref.watch(chatRemoteDataSourceProvider);
  final localDataSource = ref.watch(chatLocalDataSourceProvider);

  return ChatRepositoryImpl(
    remoteDataSource: remoteDataSource,
    localDataSource: localDataSource,
  );
});

/// Chat notifier provider
final chatProvider = StateNotifierProvider<ChatNotifier, ChatState>((ref) {
  final repository = ref.watch(chatRepositoryProvider);
  return ChatNotifier(repository);
});

/// Daily summary provider (with auto-refresh)
final dailySummaryProvider = FutureProvider<DailySummaryModel>((ref) async {
  final repository = ref.watch(chatRepositoryProvider);
  return await repository.getDailySummary();
});
