import 'package:dio/dio.dart';
import 'package:notio_app/features/auth/data/models/find_id_request.dart';
import 'package:notio_app/features/auth/data/models/find_id_response.dart';
import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/oauth_exchange_request.dart';
import 'package:notio_app/features/auth/data/models/oauth_exchange_response.dart';
import 'package:notio_app/features/auth/data/models/oauth_start_request.dart';
import 'package:notio_app/features/auth/data/models/oauth_start_response.dart';
import 'package:notio_app/features/auth/data/models/password_reset_confirm_request.dart';
import 'package:notio_app/features/auth/data/models/password_reset_confirm_response.dart';
import 'package:notio_app/features/auth/data/models/password_reset_request_request.dart';
import 'package:notio_app/features/auth/data/models/password_reset_request_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';
import 'package:notio_app/features/auth/data/models/signup_request.dart';
import 'package:notio_app/features/auth/data/models/signup_response.dart';
import 'package:notio_app/features/auth/domain/entities/auth_error_code.dart';
import 'package:notio_app/shared/constant/api_constants.dart';

/// Manual implementation of AuthApiClient (without retrofit)
class AuthApiClient {
  final Dio _dio;

  AuthApiClient(this._dio);

  Map<String, dynamic> _unwrapData(dynamic rawResponse) {
    final response = rawResponse as Map<String, dynamic>;
    if (response['success'] != true) {
      final error = response['error'] as Map<String, dynamic>?;
      throw _mapApiError(error);
    }

    return response['data'] as Map<String, dynamic>;
  }

  Never _throwMappedDioError(DioException error) {
    final responseData = error.response?.data;
    if (responseData is Map<String, dynamic>) {
      final errorBody = responseData['error'];
      if (errorBody is Map<String, dynamic>) {
        throw _mapApiError(errorBody);
      }
    }

    throw AuthError.unknown(error.message);
  }

  AuthError _mapApiError(Map<String, dynamic>? error) {
    if (error == null) {
      return AuthError.unknown();
    }

    final code = error['code']?.toString();
    final message = error['message']?.toString();

    if (code == null || code.isEmpty) {
      return AuthError.unknown(message);
    }

    return AuthError.fromApi(code: code, message: message);
  }

  Future<LoginResponse> login(LoginRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authLogin,
        data: request.toJson(),
      );
      return LoginResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<RefreshTokenResponse> refreshToken(Map<String, dynamic> body) async {
    try {
      final response = await _dio.post(
        ApiConstants.authRefresh,
        data: body,
      );
      return RefreshTokenResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<void> logout() async {
    try {
      await _dio.post(ApiConstants.authLogout);
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<SignupResponse> signup(SignupRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authSignup,
        data: request.toJson(),
      );
      return SignupResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<FindIdResponse> findId(FindIdRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authFindId,
        data: request.toJson(),
      );
      return FindIdResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<PasswordResetRequestResponse> requestPasswordReset(
      PasswordResetRequestRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authPasswordResetRequest,
        data: request.toJson(),
      );
      return PasswordResetRequestResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<PasswordResetConfirmResponse> confirmPasswordReset(
      PasswordResetConfirmRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authPasswordResetConfirm,
        data: request.toJson(),
      );
      return PasswordResetConfirmResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<OAuthStartResponse> startSocialLogin(OAuthStartRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authOAuthStart,
        data: request.toJson(),
      );
      return OAuthStartResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }

  Future<OAuthExchangeResponse> exchangeSocialLogin(
      OAuthExchangeRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.authOAuthExchange,
        data: request.toJson(),
      );
      return OAuthExchangeResponse.fromJson(_unwrapData(response.data));
    } on DioException catch (error) {
      _throwMappedDioError(error);
    }
  }
}
