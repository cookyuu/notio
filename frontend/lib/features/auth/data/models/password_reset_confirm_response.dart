import 'package:json_annotation/json_annotation.dart';

part 'password_reset_confirm_response.g.dart';

@JsonSerializable()
class PasswordResetConfirmResponse {
  final String message;

  const PasswordResetConfirmResponse({
    required this.message,
  });

  factory PasswordResetConfirmResponse.fromJson(Map<String, dynamic> json) =>
      _$PasswordResetConfirmResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PasswordResetConfirmResponseToJson(this);
}
