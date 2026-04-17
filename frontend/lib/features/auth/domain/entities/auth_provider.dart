/// Authentication provider type
enum AuthProvider {
  local,
  google,
  apple,
  kakao,
  naver;

  /// Convert to uppercase string for API request
  String toApiString() {
    return name.toUpperCase();
  }

  /// Parse from API response (uppercase)
  static AuthProvider fromApiString(String value) {
    return AuthProvider.values.firstWhere(
      (e) => e.name.toUpperCase() == value.toUpperCase(),
      orElse: () => throw ArgumentError('Invalid AuthProvider: $value'),
    );
  }
}
