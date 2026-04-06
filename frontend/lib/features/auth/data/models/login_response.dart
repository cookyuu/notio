import 'package:json_annotation/json_annotation.dart';

part 'login_response.g.dart';

@JsonSerializable()
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

  factory LoginResponse.fromJson(Map<String, dynamic> json) =>
      _$LoginResponseFromJson(json);

  Map<String, dynamic> toJson() => _$LoginResponseToJson(this);

  DateTime get expiresAt =>
      DateTime.now().add(Duration(seconds: expiresIn));
}
