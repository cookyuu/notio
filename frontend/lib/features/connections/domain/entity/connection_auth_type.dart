/// Connection authentication type enum
/// Backend는 UPPER_SNAKE_CASE를 사용하지만 Dart enum은 camelCase를 사용
enum ConnectionAuthType {
  apiKey('API_KEY'),
  oauth('OAUTH'),
  signature('SIGNATURE'),
  system('SYSTEM');

  const ConnectionAuthType(this.serverValue);

  /// Backend API가 사용하는 UPPER_SNAKE_CASE 값
  final String serverValue;

  /// Backend 값을 Frontend enum으로 변환
  static ConnectionAuthType fromServerValue(String value) {
    return ConnectionAuthType.values.firstWhere(
      (e) => e.serverValue == value,
      orElse: () => throw ArgumentError('Unknown ConnectionAuthType: $value'),
    );
  }

  /// Frontend enum을 Backend 값으로 변환
  String toServerValue() => serverValue;
}
