class RefreshTokenResponse {
  final String accessToken;
  final String refreshToken;
  final String? tokenType;
  final int expiresIn; // seconds

  const RefreshTokenResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
    this.tokenType,
  });

  factory RefreshTokenResponse.fromJson(Map<String, dynamic> json) {
    return RefreshTokenResponse(
      accessToken: (json['accessToken'] ?? json['access_token']) as String,
      refreshToken: (json['refreshToken'] ?? json['refresh_token']) as String,
      tokenType: (json['tokenType'] ?? json['token_type']) as String?,
      expiresIn: ((json['expiresIn'] ?? json['expires_in']) as num).toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
        'accessToken': accessToken,
        'refreshToken': refreshToken,
        if (tokenType != null) 'tokenType': tokenType,
        'expiresIn': expiresIn,
      };

  DateTime get expiresAt =>
      DateTime.now().add(Duration(seconds: expiresIn));
}
