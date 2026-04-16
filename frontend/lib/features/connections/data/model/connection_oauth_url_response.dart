import 'package:json_annotation/json_annotation.dart';

part 'connection_oauth_url_response.g.dart';

/// OAuth URL 응답 DTO
@JsonSerializable()
class ConnectionOAuthUrlResponse {
  @JsonKey(name: 'authorization_url')
  final String authorizationUrl;
  @JsonKey(name: 'state_expires_in')
  final int stateExpiresIn;

  const ConnectionOAuthUrlResponse({
    required this.authorizationUrl,
    required this.stateExpiresIn,
  });

  factory ConnectionOAuthUrlResponse.fromJson(Map<String, dynamic> json) =>
      _$ConnectionOAuthUrlResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionOAuthUrlResponseToJson(this);
}
