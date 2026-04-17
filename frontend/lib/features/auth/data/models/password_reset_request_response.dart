import 'package:json_annotation/json_annotation.dart';

part 'password_reset_request_response.g.dart';

@JsonSerializable()
class PasswordResetRequestResponse {
  final String message;

  const PasswordResetRequestResponse({
    required this.message,
  });

  factory PasswordResetRequestResponse.fromJson(Map<String, dynamic> json) =>
      _$PasswordResetRequestResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PasswordResetRequestResponseToJson(this);
}
