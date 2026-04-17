import 'package:json_annotation/json_annotation.dart';

part 'signup_response.g.dart';

@JsonSerializable()
class SignupResponse {
  @JsonKey(name: 'user_id')
  final String userId;
  final String email;
  @JsonKey(name: 'display_name')
  final String displayName;

  const SignupResponse({
    required this.userId,
    required this.email,
    required this.displayName,
  });

  factory SignupResponse.fromJson(Map<String, dynamic> json) =>
      _$SignupResponseFromJson(json);

  Map<String, dynamic> toJson() => _$SignupResponseToJson(this);
}
