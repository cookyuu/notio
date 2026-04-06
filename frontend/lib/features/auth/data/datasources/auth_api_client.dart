import 'package:dio/dio.dart';
import 'package:retrofit/retrofit.dart';
import 'package:notio_app/features/auth/data/models/login_request.dart';
import 'package:notio_app/features/auth/data/models/login_response.dart';
import 'package:notio_app/features/auth/data/models/refresh_token_response.dart';

part 'auth_api_client.g.dart';

@RestApi()
abstract class AuthApiClient {
  factory AuthApiClient(Dio dio, {String baseUrl}) = _AuthApiClient;

  @POST('/api/v1/auth/login')
  Future<LoginResponse> login(@Body() LoginRequest request);

  @POST('/api/v1/auth/refresh')
  Future<RefreshTokenResponse> refreshToken(
    @Body() Map<String, dynamic> body,
  );

  @POST('/api/v1/auth/logout')
  Future<void> logout();
}
