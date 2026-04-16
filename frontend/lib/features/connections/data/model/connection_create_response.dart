import 'package:json_annotation/json_annotation.dart';
import 'connection_model.dart';

part 'connection_create_response.g.dart';

/// Connection 생성 응답 DTO
/// API Key 생성 시에만 api_key 필드에 원문 key가 포함됨 (1회만)
@JsonSerializable()
class ConnectionCreateResponse {
  final ConnectionModel connection;
  @JsonKey(name: 'api_key')
  final String? apiKey;

  const ConnectionCreateResponse({
    required this.connection,
    this.apiKey,
  });

  factory ConnectionCreateResponse.fromJson(Map<String, dynamic> json) =>
      _$ConnectionCreateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionCreateResponseToJson(this);
}
