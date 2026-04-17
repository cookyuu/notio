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

  Future<SignupResponse> signup(SignupRequest request) async {
    final response = await _dio.post(
      ApiConstants.authSignup,
      data: request.toJson(),
    );
    return SignupResponse.fromJson(_unwrapData(response.data));
  }

  Future<FindIdResponse> findId(FindIdRequest request) async {
    final response = await _dio.post(
      ApiConstants.authFindId,
      data: request.toJson(),
    );
    return FindIdResponse.fromJson(_unwrapData(response.data));
  }

  Future<PasswordResetRequestResponse> requestPasswordReset(
      PasswordResetRequestRequest request) async {
    final response = await _dio.post(
      ApiConstants.authPasswordResetRequest,
      data: request.toJson(),
    );
    return PasswordResetRequestResponse.fromJson(_unwrapData(response.data));
  }

  Future<PasswordResetConfirmResponse> confirmPasswordReset(
      PasswordResetConfirmRequest request) async {
    final response = await _dio.post(
      ApiConstants.authPasswordResetConfirm,
      data: request.toJson(),
    );
    return PasswordResetConfirmResponse.fromJson(_unwrapData(response.data));
  }

  Future<OAuthStartResponse> startSocialLogin(OAuthStartRequest request) async {
    final response = await _dio.post(
      ApiConstants.authOAuthStart,
      data: request.toJson(),
    );
    return OAuthStartResponse.fromJson(_unwrapData(response.data));
  }

  Future<OAuthExchangeResponse> exchangeSocialLogin(
      OAuthExchangeRequest request) async {
    final response = await _dio.post(
      ApiConstants.authOAuthExchange,
      data: request.toJson(),
    );
    return OAuthExchangeResponse.fromJson(_unwrapData(response.data));
  }
}
