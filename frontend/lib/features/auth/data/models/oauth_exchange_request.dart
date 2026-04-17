import 'package:json_annotation/json_annotation.dart';
import '../../domain/entities/auth_provider.dart';

part 'oauth_exchange_request.g.dart';

@JsonSerializable()
class OAuthExchangeRequest {
  @JsonKey(
    toJson: _authProviderToJson,
    fromJson: _authProviderFromJson,
  )
  final AuthProvider provider;

  final String state;
  final String code;

  const OAuthExchangeRequest({
    required this.provider,
    required this.state,
    required this.code,
  });

  factory OAuthExchangeRequest.fromJson(Map<String, dynamic> json) =>
      _$OAuthExchangeRequestFromJson(json);

  Map<String, dynamic> toJson() => _$OAuthExchangeRequestToJson(this);

  static String _authProviderToJson(AuthProvider provider) =>
      provider.toApiString();

  static AuthProvider _authProviderFromJson(String value) =>
      AuthProvider.fromApiString(value);
}
