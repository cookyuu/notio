import 'package:dio/dio.dart';
import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';

/// Manual implementation of AuthApiClient (without retrofit)
class AuthApiClient {
  final Dio _dio;
  final String _baseUrl;

  AuthApiClient(this._dio, {String? baseUrl})
      : _baseUrl = baseUrl ?? 'http://localhost:8080';

  Future<LoginResponse> login(LoginRequest request) async {
    final response = await _dio.post(
      '$_baseUrl/api/v1/auth/login',
      data: request.toJson(),
    );
    return LoginResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<RefreshTokenResponse> refreshToken(Map<String, dynamic> body) async {
    final response = await _dio.post(
      '$_baseUrl/api/v1/auth/refresh',
      data: body,
    );
    return RefreshTokenResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> logout() async {
    await _dio.post('$_baseUrl/api/v1/auth/logout');
  }
}
