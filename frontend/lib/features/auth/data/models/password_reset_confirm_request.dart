import 'package:json_annotation/json_annotation.dart';

part 'password_reset_confirm_request.g.dart';

@JsonSerializable()
class PasswordResetConfirmRequest {
  final String token;
  @JsonKey(name: 'new_password')
  final String newPassword;

  const PasswordResetConfirmRequest({
    required this.token,
    required this.newPassword,
  });

  factory PasswordResetConfirmRequest.fromJson(Map<String, dynamic> json) =>
      _$PasswordResetConfirmRequestFromJson(json);

  Map<String, dynamic> toJson() => _$PasswordResetConfirmRequestToJson(this);
}
