import 'package:json_annotation/json_annotation.dart';
import '../../domain/entities/auth_platform.dart';
import '../../domain/entities/auth_provider.dart';

part 'oauth_callback_response.g.dart';

@JsonSerializable()
class OAuthCallbackResponse {
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

  @JsonKey(name: 'redirect_uri')
  final String redirectUri;

  final String message;

  const OAuthCallbackResponse({
    required this.provider,
    required this.platform,
    required this.state,
    required this.redirectUri,
    required this.message,
  });

  factory OAuthCallbackResponse.fromJson(Map<String, dynamic> json) =>
      _$OAuthCallbackResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OAuthCallbackResponseToJson(this);

  static String _authProviderToJson(AuthProvider provider) =>
      provider.toApiString();

  static AuthProvider _authProviderFromJson(String value) =>
      AuthProvider.fromApiString(value);

  static String _authPlatformToJson(AuthPlatform platform) =>
      platform.toApiString();

  static AuthPlatform _authPlatformFromJson(String value) =>
      AuthPlatform.fromApiString(value);
}
