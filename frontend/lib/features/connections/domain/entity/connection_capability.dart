/// Connection capability enum
/// Backend는 UPPER_SNAKE_CASE를 사용하지만 Dart enum은 camelCase를 사용
enum ConnectionCapability {
  webhookReceive('WEBHOOK_RECEIVE'),
  testMessage('TEST_MESSAGE'),
  refreshToken('REFRESH_TOKEN'),
  rotateKey('ROTATE_KEY');

  const ConnectionCapability(this.serverValue);

  /// Backend API가 사용하는 UPPER_SNAKE_CASE 값
  final String serverValue;

  /// Backend 값을 Frontend enum으로 변환
  static ConnectionCapability fromServerValue(String value) {
    return ConnectionCapability.values.firstWhere(
      (e) => e.serverValue == value,
      orElse: () => throw ArgumentError('Unknown ConnectionCapability: $value'),
    );
  }

  /// Frontend enum을 Backend 값으로 변환
  String toServerValue() => serverValue;

  /// Backend 배열을 Frontend enum 배열로 변환
  static List<ConnectionCapability> fromServerValues(List<String> values) {
    return values.map((v) => fromServerValue(v)).toList();
  }

  /// Frontend enum 배열을 Backend 값 배열로 변환
  static List<String> toServerValues(List<ConnectionCapability> capabilities) {
    return capabilities.map((c) => c.serverValue).toList();
  }
}
