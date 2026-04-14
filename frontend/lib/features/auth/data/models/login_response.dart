class LoginResponse {
  final String userId;
  final String email;
  final String accessToken;
  final String refreshToken;
  final int expiresIn; // seconds

  const LoginResponse({
    required this.userId,
    required this.email,
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      userId: json['user_id'].toString(),
      email: json['email'] as String,
      accessToken: json['access_token'] as String,
      refreshToken: json['refresh_token'] as String,
      expiresIn: (json['expires_in'] as num).toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
        'user_id': userId,
        'email': email,
        'access_token': accessToken,
        'refresh_token': refreshToken,
        'expires_in': expiresIn,
      };

  DateTime get expiresAt =>
      DateTime.now().add(Duration(seconds: expiresIn));
}
