import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/connection_provider.dart';

part 'connection_oauth_url_request.g.dart';

/// OAuth URL 요청 DTO
@JsonSerializable()
class ConnectionOAuthUrlRequest {
  final String provider;
  @JsonKey(name: 'display_name')
  final String displayName;
  @JsonKey(name: 'redirect_uri')
  final String? redirectUri;

  const ConnectionOAuthUrlRequest({
    required this.provider,
    required this.displayName,
    this.redirectUri,
  });

  factory ConnectionOAuthUrlRequest.fromJson(Map<String, dynamic> json) =>
      _$ConnectionOAuthUrlRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionOAuthUrlRequestToJson(this);

  /// Create from domain values
  factory ConnectionOAuthUrlRequest.create({
    required ConnectionProvider provider,
    required String displayName,
    String? redirectUri,
  }) {
    return ConnectionOAuthUrlRequest(
      provider: provider.toServerValue(),
      displayName: displayName,
      redirectUri: redirectUri,
    );
  }
}
