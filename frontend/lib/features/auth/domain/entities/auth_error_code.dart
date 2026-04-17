/// Authentication error codes from backend
enum AuthErrorCode {
  // 400 Bad Request
  invalidRequest,
  invalidInputValue,
  emailAlreadyExists,
  passwordResetTokenInvalid,
  passwordResetTokenExpired,
  oauthStateInvalid,
  oauthCallbackFailed,
  authProviderUnsupported,

  // 401 Unauthorized
  unauthorized,
  invalidCredentials,
  invalidToken,
  expiredToken,

  // 403 Forbidden
  forbidden,

  // 429 Too Many Requests
  rateLimitExceeded,

  // 500 Internal Server Error
  internalServerError,

  // Unknown error
  unknown;

  /// Convert from API error code string
  static AuthErrorCode fromApiCode(String code) {
    switch (code.toUpperCase()) {
      // 400 Bad Request
      case 'INVALID_REQUEST':
        return AuthErrorCode.invalidRequest;
      case 'INVALID_INPUT_VALUE':
        return AuthErrorCode.invalidInputValue;
      case 'EMAIL_ALREADY_EXISTS':
        return AuthErrorCode.emailAlreadyExists;
      case 'PASSWORD_RESET_TOKEN_INVALID':
        return AuthErrorCode.passwordResetTokenInvalid;
      case 'PASSWORD_RESET_TOKEN_EXPIRED':
        return AuthErrorCode.passwordResetTokenExpired;
      case 'OAUTH_STATE_INVALID':
        return AuthErrorCode.oauthStateInvalid;
      case 'OAUTH_CALLBACK_FAILED':
        return AuthErrorCode.oauthCallbackFailed;
      case 'AUTH_PROVIDER_UNSUPPORTED':
        return AuthErrorCode.authProviderUnsupported;

      // 401 Unauthorized
      case 'UNAUTHORIZED':
        return AuthErrorCode.unauthorized;
      case 'INVALID_CREDENTIALS':
        return AuthErrorCode.invalidCredentials;
      case 'INVALID_TOKEN':
        return AuthErrorCode.invalidToken;
      case 'EXPIRED_TOKEN':
        return AuthErrorCode.expiredToken;

      // 403 Forbidden
      case 'FORBIDDEN':
        return AuthErrorCode.forbidden;

      // 429 Too Many Requests
      case 'RATE_LIMIT_EXCEEDED':
        return AuthErrorCode.rateLimitExceeded;

      // 500 Internal Server Error
      case 'INTERNAL_SERVER_ERROR':
        return AuthErrorCode.internalServerError;

      default:
        return AuthErrorCode.unknown;
    }
  }

  /// Get user-friendly error message in Korean
  String get message {
    switch (this) {
      // 400 Bad Request
      case AuthErrorCode.invalidRequest:
        return '잘못된 요청입니다.';
      case AuthErrorCode.invalidInputValue:
        return '비밀번호 형식이 올바르지 않습니다. 영문, 숫자, 특수문자를 포함해 다시 입력해주세요.';
      case AuthErrorCode.emailAlreadyExists:
        return '이미 사용 중인 이메일입니다.';
      case AuthErrorCode.passwordResetTokenInvalid:
        return '유효하지 않은 비밀번호 재설정 링크입니다.';
      case AuthErrorCode.passwordResetTokenExpired:
        return '비밀번호 재설정 링크가 만료되었습니다.';
      case AuthErrorCode.oauthStateInvalid:
        return '소셜 로그인 인증에 실패했습니다.';
      case AuthErrorCode.oauthCallbackFailed:
        return '소셜 로그인 처리에 실패했습니다.';
      case AuthErrorCode.authProviderUnsupported:
        return '지원하지 않는 로그인 방식입니다.';

      // 401 Unauthorized
      case AuthErrorCode.unauthorized:
        return '인증에 실패했습니다.';
      case AuthErrorCode.invalidCredentials:
        return '이메일 또는 비밀번호가 올바르지 않습니다.';
      case AuthErrorCode.invalidToken:
        return '유효하지 않은 토큰입니다.';
      case AuthErrorCode.expiredToken:
        return '토큰이 만료되었습니다. 다시 로그인해주세요.';

      // 403 Forbidden
      case AuthErrorCode.forbidden:
        return '접근 권한이 없습니다.';

      // 429 Too Many Requests
      case AuthErrorCode.rateLimitExceeded:
        return '요청 횟수 제한을 초과했습니다. 잠시 후 다시 시도해주세요.';

      // 500 Internal Server Error
      case AuthErrorCode.internalServerError:
        return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';

      // Unknown
      case AuthErrorCode.unknown:
        return '알 수 없는 오류가 발생했습니다.';
    }
  }
}

/// Authentication error class
class AuthError {
  final AuthErrorCode code;
  final String message;
  final String? originalMessage;

  const AuthError({
    required this.code,
    required this.message,
    this.originalMessage,
  });

  /// Create from API error response
  factory AuthError.fromApi({
    required String code,
    String? message,
  }) {
    final errorCode = AuthErrorCode.fromApiCode(code);
    return AuthError(
      code: errorCode,
      message: errorCode.message,
      originalMessage: message,
    );
  }

  /// Create unknown error
  factory AuthError.unknown([String? message]) {
    return AuthError(
      code: AuthErrorCode.unknown,
      message: AuthErrorCode.unknown.message,
      originalMessage: message,
    );
  }

  @override
  String toString() => message;
}
