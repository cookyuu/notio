import 'package:dio/dio.dart';
import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';
import 'package:notio_app/shared/constant/api_constants.dart';

/// Manual implementation of AuthApiClient (without retrofit)
class AuthApiClient {
  final Dio _dio;

  AuthApiClient(this._dio);

  Map<String, dynamic> _unwrapData(dynamic rawResponse) {
    final response = rawResponse as Map<String, dynamic>;
    if (response['success'] != true) {
      final error = response['error'] as Map<String, dynamic>?;
      throw Exception(error?['message'] ?? 'API request failed');
    }

    return response['data'] as Map<String, dynamic>;
  }

  Future<LoginResponse> login(LoginRequest request) async {
    final response = await _dio.post(
      ApiConstants.authLogin,
      data: request.toJson(),
    );
    return LoginResponse.fromJson(_unwrapData(response.data));
  }

  Future<RefreshTokenResponse> refreshToken(Map<String, dynamic> body) async {
    final response = await _dio.post(
      ApiConstants.authRefresh,
      data: body,
    );
    return RefreshTokenResponse.fromJson(_unwrapData(response.data));
  }

  Future<void> logout() async {
    await _dio.post(ApiConstants.authLogout);
  }
}
