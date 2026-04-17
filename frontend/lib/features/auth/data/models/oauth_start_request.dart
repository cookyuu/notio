import 'package:json_annotation/json_annotation.dart';
import '../../domain/entities/auth_platform.dart';
import '../../domain/entities/auth_provider.dart';

part 'oauth_start_request.g.dart';

@JsonSerializable()
class OAuthStartRequest {
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

  @JsonKey(name: 'redirect_uri')
  final String redirectUri;

  @JsonKey(name: 'pkce_verifier')
  final String? pkceVerifier;

  const OAuthStartRequest({
    required this.provider,
    required this.platform,
    required this.redirectUri,
    this.pkceVerifier,
  });

  factory OAuthStartRequest.fromJson(Map<String, dynamic> json) =>
      _$OAuthStartRequestFromJson(json);

  Map<String, dynamic> toJson() => _$OAuthStartRequestToJson(this);

  static String _authProviderToJson(AuthProvider provider) =>
      provider.toApiString();

  static AuthProvider _authProviderFromJson(String value) =>
      AuthProvider.fromApiString(value);

  static String _authPlatformToJson(AuthPlatform platform) =>
      platform.toApiString();

  static AuthPlatform _authPlatformFromJson(String value) =>
      AuthPlatform.fromApiString(value);
}
