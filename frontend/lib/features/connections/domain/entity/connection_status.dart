/// Connection status enum
/// Backend는 UPPER_SNAKE_CASE를 사용하지만 Dart enum은 camelCase를 사용
enum ConnectionStatus {
  pending('PENDING'),
  active('ACTIVE'),
  needsAction('NEEDS_ACTION'),
  revoked('REVOKED'),
  error('ERROR');

  const ConnectionStatus(this.serverValue);

  /// Backend API가 사용하는 UPPER_SNAKE_CASE 값
  final String serverValue;

  /// Backend 값을 Frontend enum으로 변환
  static ConnectionStatus fromServerValue(String value) {
    return ConnectionStatus.values.firstWhere(
      (e) => e.serverValue == value,
      orElse: () => throw ArgumentError('Unknown ConnectionStatus: $value'),
    );
  }

  /// Frontend enum을 Backend 값으로 변환
  String toServerValue() => serverValue;
}
