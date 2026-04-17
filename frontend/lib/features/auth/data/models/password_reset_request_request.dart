import 'package:json_annotation/json_annotation.dart';

part 'password_reset_request_request.g.dart';

@JsonSerializable()
class PasswordResetRequestRequest {
  final String email;

  const PasswordResetRequestRequest({
    required this.email,
  });

  factory PasswordResetRequestRequest.fromJson(Map<String, dynamic> json) =>
      _$PasswordResetRequestRequestFromJson(json);

  Map<String, dynamic> toJson() => _$PasswordResetRequestRequestToJson(this);
}
