class LoginResponse {
  final String userId;
  final String email;
  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final int expiresIn; // seconds

  const LoginResponse({
    required this.userId,
    required this.email,
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.expiresIn,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    final user = json['user'] as Map<String, dynamic>;

    return LoginResponse(
      userId: user['id'].toString(),
      email: user['primary_email'] as String,
      accessToken: json['access_token'] as String,
      refreshToken: json['refresh_token'] as String,
      tokenType: json['token_type'] as String,
      expiresIn: (json['expires_in'] as num).toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
        'user_id': userId,
        'email': email,
        'access_token': accessToken,
        'refresh_token': refreshToken,
        'token_type': tokenType,
        'expires_in': expiresIn,
      };

  DateTime get expiresAt =>
      DateTime.now().add(Duration(seconds: expiresIn));
}
