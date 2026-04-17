/// Authentication platform type
enum AuthPlatform {
  web,
  ios,
  android;

  /// Convert to uppercase string for API request
  String toApiString() {
    return name.toUpperCase();
  }

  /// Parse from API response (uppercase)
  static AuthPlatform fromApiString(String value) {
    return AuthPlatform.values.firstWhere(
      (e) => e.name.toUpperCase() == value.toUpperCase(),
      orElse: () => throw ArgumentError('Invalid AuthPlatform: $value'),
    );
  }
}
