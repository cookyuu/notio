import 'package:json_annotation/json_annotation.dart';
import 'connection_model.dart';

part 'connection_rotate_key_response.g.dart';

/// API Key 재발급 응답 DTO
/// 새 원문 API Key는 이 응답에서만 1회 반환됨
@JsonSerializable()
class ConnectionRotateKeyResponse {
  final ConnectionModel connection;
  @JsonKey(name: 'api_key')
  final String apiKey;

  const ConnectionRotateKeyResponse({
    required this.connection,
    required this.apiKey,
  });

  factory ConnectionRotateKeyResponse.fromJson(Map<String, dynamic> json) =>
      _$ConnectionRotateKeyResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionRotateKeyResponseToJson(this);
}
