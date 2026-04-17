import 'package:json_annotation/json_annotation.dart';

part 'signup_request.g.dart';

@JsonSerializable()
class SignupRequest {
  final String email;
  final String password;
  @JsonKey(name: 'display_name')
  final String displayName;

  const SignupRequest({
    required this.email,
    required this.password,
    required this.displayName,
  });

  factory SignupRequest.fromJson(Map<String, dynamic> json) =>
      _$SignupRequestFromJson(json);

  Map<String, dynamic> toJson() => _$SignupRequestToJson(this);
}
