import 'package:json_annotation/json_annotation.dart';
import '../../domain/entities/auth_provider.dart';

part 'oauth_exchange_response.g.dart';

@JsonSerializable()
class OAuthExchangeResponse {
  @JsonKey(
    toJson: _authProviderToJson,
    fromJson: _authProviderFromJson,
  )
  final AuthProvider provider;

  final String state;
  final String message;

  const OAuthExchangeResponse({
    required this.provider,
    required this.state,
    required this.message,
  });

  factory OAuthExchangeResponse.fromJson(Map<String, dynamic> json) =>
      _$OAuthExchangeResponseFromJson(json);

  Map<String, dynamic> toJson() => _$OAuthExchangeResponseToJson(this);

  static String _authProviderToJson(AuthProvider provider) =>
      provider.toApiString();

  static AuthProvider _authProviderFromJson(String value) =>
      AuthProvider.fromApiString(value);
}
