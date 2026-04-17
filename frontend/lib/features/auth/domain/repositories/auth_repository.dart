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
import 'package:notio_app/features/auth/data/models/signup_request.dart';
import 'package:notio_app/features/auth/data/models/signup_response.dart';

/// Auth repository interface
abstract class AuthRepository {
  /// Login with email and password
  Future<LoginResponse> login(LoginRequest request);

  /// Refresh access token
  Future<void> refreshToken();

  /// Logout
  Future<void> logout();

  /// Check if user is logged in
  Future<bool> isLoggedIn();

  /// Get stored access token
  Future<String?> getAccessToken();

  /// Get stored refresh token
  Future<String?> getRefreshToken();

  /// Get user email
  Future<String?> getUserEmail();

  /// Sign up with email and password
  Future<SignupResponse> signup(SignupRequest request);

  /// Find user ID by email
  Future<FindIdResponse> findId(FindIdRequest request);

  /// Request password reset
  Future<PasswordResetRequestResponse> requestPasswordReset(
      PasswordResetRequestRequest request);

  /// Confirm password reset with token
  Future<PasswordResetConfirmResponse> confirmPasswordReset(
      PasswordResetConfirmRequest request);

  /// Start social login (OAuth)
  Future<OAuthStartResponse> startSocialLogin(OAuthStartRequest request);

  /// Exchange OAuth code for tokens
  Future<OAuthExchangeResponse> exchangeSocialLogin(
      OAuthExchangeRequest request);
}
