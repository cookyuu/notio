import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Interceptor for adding JWT token to requests
class AuthInterceptor extends Interceptor {
  final FlutterSecureStorage _storage = const FlutterSecureStorage();
  static const String _tokenKey = 'auth_token';

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    // Get token from secure storage
    final token = await _storage.read(key: _tokenKey);

    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }

    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Handle 401 Unauthorized - token expired or invalid
    if (err.response?.statusCode == 401) {
      // TODO: Navigate to login screen or refresh token
      _storage.delete(key: _tokenKey);
    }

    // Handle 5xx errors - retry with exponential backoff
    if (err.response?.statusCode != null &&
        err.response!.statusCode! >= 500) {
      // TODO: Implement retry logic with exponential backoff
    }

    handler.next(err);
  }
}
