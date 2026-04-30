/// Connection provider enum
/// Backend는 UPPER_SNAKE_CASE를 사용하지만 Dart enum은 camelCase를 사용
enum ConnectionProvider {
  claude('CLAUDE'),
  codex('CODEX'),
  slack('SLACK'),
  gmail('GMAIL'),
  github('GITHUB'),
  discord('DISCORD'),
  jira('JIRA'),
  linear('LINEAR'),
  teams('TEAMS');

  const ConnectionProvider(this.serverValue);

  /// Backend API가 사용하는 UPPER_SNAKE_CASE 값
  final String serverValue;

  /// Backend 값을 Frontend enum으로 변환
  static ConnectionProvider fromServerValue(String value) {
    return ConnectionProvider.values.firstWhere(
      (e) => e.serverValue == value,
      orElse: () => throw ArgumentError('Unknown ConnectionProvider: $value'),
    );
  }

  /// Frontend enum을 Backend 값으로 변환
  String toServerValue() => serverValue;
}
