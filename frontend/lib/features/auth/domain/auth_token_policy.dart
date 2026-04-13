class AuthTokenPolicy {
  AuthTokenPolicy._();

  static const String _mockTokenPrefix = 'mock-';

  static bool isMockToken(String? token) {
    return token != null && token.startsWith(_mockTokenPrefix);
  }
}
