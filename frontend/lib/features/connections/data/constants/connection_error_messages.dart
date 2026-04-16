/// Connection 관련 에러 메시지 상수
class ConnectionErrorMessages {
  ConnectionErrorMessages._();

  // Rate Limit
  static const rateLimitExceeded = '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.';
  static String rateLimitExceededWithRetry(int retryAfterSeconds) =>
      '요청이 너무 많습니다. $retryAfterSeconds초 후에 다시 시도해주세요.';

  // Connection
  static const connectionNotFound = '연동 정보를 찾을 수 없습니다.';
  static const connectionProviderUnsupported = '지원하지 않는 연동 서비스입니다.';
  static const connectionAuthTypeUnsupported = '지원하지 않는 인증 방식입니다.';
  static const connectionAlreadyExists = '이미 동일한 연동이 존재합니다.';
  static const connectionVerificationFailed = '연동 검증에 실패했습니다.';

  // OAuth
  static const oauthStateInvalid = 'OAuth 인증 상태가 유효하지 않습니다.';
  static const oauthCallbackFailed = 'OAuth 인증 처리에 실패했습니다.';

  // Webhook
  static const webhookKeyMissing = 'Webhook API Key가 없습니다.';
  static const webhookKeyInvalid = 'Webhook API Key가 유효하지 않습니다.';
  static const webhookKeyExpired = 'Webhook API Key가 만료되었습니다.';
  static const webhookKeyRevoked = 'Webhook API Key가 폐기되었습니다.';
  static const webhookSourceMismatch = 'Webhook 요청 출처가 일치하지 않습니다.';
  static const webhookVerificationFailed = 'Webhook 검증에 실패했습니다.';

  // Provider Signature
  static const providerSignatureInvalid = '서비스 제공자 서명 검증에 실패했습니다.';

  // Payload
  static const payloadTooLarge = '요청 데이터 크기가 제한을 초과했습니다.';

  // Generic
  static const invalidRequest = '잘못된 요청입니다.';
  static const internalServerError = '서버 내부 오류가 발생했습니다.';

  /// 에러 코드를 사용자 친화적인 메시지로 변환
  static String fromErrorCode(String errorCode) {
    switch (errorCode) {
      case 'RATE_LIMIT_EXCEEDED':
        return rateLimitExceeded;
      case 'CONNECTION_NOT_FOUND':
        return connectionNotFound;
      case 'CONNECTION_PROVIDER_UNSUPPORTED':
        return connectionProviderUnsupported;
      case 'CONNECTION_AUTH_TYPE_UNSUPPORTED':
        return connectionAuthTypeUnsupported;
      case 'CONNECTION_ALREADY_EXISTS':
        return connectionAlreadyExists;
      case 'CONNECTION_VERIFICATION_FAILED':
        return connectionVerificationFailed;
      case 'OAUTH_STATE_INVALID':
        return oauthStateInvalid;
      case 'OAUTH_CALLBACK_FAILED':
        return oauthCallbackFailed;
      case 'WEBHOOK_KEY_MISSING':
        return webhookKeyMissing;
      case 'WEBHOOK_KEY_INVALID':
        return webhookKeyInvalid;
      case 'WEBHOOK_KEY_EXPIRED':
        return webhookKeyExpired;
      case 'WEBHOOK_KEY_REVOKED':
        return webhookKeyRevoked;
      case 'WEBHOOK_SOURCE_MISMATCH':
        return webhookSourceMismatch;
      case 'WEBHOOK_VERIFICATION_FAILED':
        return webhookVerificationFailed;
      case 'PROVIDER_SIGNATURE_INVALID':
        return providerSignatureInvalid;
      case 'PAYLOAD_TOO_LARGE':
        return payloadTooLarge;
      case 'INVALID_REQUEST':
        return invalidRequest;
      case 'INTERNAL_SERVER_ERROR':
        return internalServerError;
      default:
        return '알 수 없는 오류가 발생했습니다.';
    }
  }
}

/// Retry-After header를 사용자 메시지에 반영할지 결정
///
/// 429 응답 시 backend는 Retry-After header를 포함합니다.
/// Frontend는 이를 파싱하여 사용자에게 재시도 가능 시간을 표시합니다.
///
/// 사용 예:
/// ```dart
/// if (response.statusCode == 429) {
///   final retryAfter = response.headers['retry-after'];
///   if (retryAfter != null) {
///     final seconds = int.tryParse(retryAfter);
///     if (seconds != null) {
///       return ConnectionErrorMessages.rateLimitExceededWithRetry(seconds);
///     }
///   }
///   return ConnectionErrorMessages.rateLimitExceeded;
/// }
/// ```
