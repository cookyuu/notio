import 'package:dio/dio.dart';
import 'package:notio_app/core/network/auth_interceptor.dart';
import 'package:notio_app/core/network/logging_interceptor.dart';

/// Dio client configuration
class DioClient {
  DioClient._();

  static Dio create({
    required String baseUrl,
    bool enableLogging = true,
    Duration connectTimeout = const Duration(seconds: 30),
    Duration receiveTimeout = const Duration(seconds: 30),
    Duration? sendTimeout,
  }) {
    final dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: connectTimeout,
        receiveTimeout: receiveTimeout,
        sendTimeout: sendTimeout,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // Add interceptors
    dio.interceptors.addAll([
      AuthInterceptor(),
      if (enableLogging) LoggingInterceptor(),
    ]);

    return dio;
  }
}
