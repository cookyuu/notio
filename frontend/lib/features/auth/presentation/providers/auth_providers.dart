import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:notio_app/core/network/dio_client.dart';
import 'package:notio_app/features/auth/data/datasources/auth_api_client.dart';
import 'package:notio_app/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:notio_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:notio_app/shared/constant/api_constants.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'auth_providers.g.dart';

/// Provide FlutterSecureStorage instance
@riverpod
FlutterSecureStorage secureStorage(SecureStorageRef ref) {
  return const FlutterSecureStorage();
}

/// Provide AuthApiClient instance
@riverpod
AuthApiClient authApiClient(AuthApiClientRef ref) {
  final dio = DioClient.create(
    baseUrl: ApiConstants.baseUrl,
  );
  return AuthApiClient(dio);
}

/// Provide AuthRepository instance
@riverpod
AuthRepository authRepository(AuthRepositoryRef ref) {
  return AuthRepositoryImpl(
    apiClient: ref.watch(authApiClientProvider),
    storage: ref.watch(secureStorageProvider),
  );
}
