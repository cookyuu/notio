import 'package:json_annotation/json_annotation.dart';
import '../../domain/entities/auth_platform.dart';
import '../../domain/entities/auth_provider.dart';

part 'oauth_start_response.g.dart';

@JsonSerializable()
class OAuthStartResponse {
  @JsonKey(
    toJson: _authProviderToJson,
    fromJson: _authProviderFromJson,
  )
  final AuthProvider provider;

  @JsonKey(
    toJson: _authPlatformToJson,
    fromJson: _authPlatformFromJson,
  )
  final AuthPlatform platform;

  final String state;

  @JsonKey(name: 'authorization_url')
  final String authorizationUrl;

  @JsonKey(name: 'expires_at')
  final DateTime expiresAt;

  const OAuthStartResponse({
    required this.provider,
    required this.platform,
    required this.state,
    required this.authorizationUrl,
    required this.expiresAt,
  });

  factory OAuthStartResponse.fromJson(Map<String, dynamic> json) =>
      _$OAuthStartResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OAuthStartResponseToJson(this);

  static String _authProviderToJson(AuthProvider provider) =>
      provider.toApiString();

  static AuthProvider _authProviderFromJson(String value) =>
      AuthProvider.fromApiString(value);

  static String _authPlatformToJson(AuthPlatform platform) =>
      platform.toApiString();

  static AuthPlatform _authPlatformFromJson(String value) =>
      AuthPlatform.fromApiString(value);
}
