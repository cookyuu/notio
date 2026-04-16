import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_auth_type.dart';

part 'connection_create_request.g.dart';

/// Connection 생성 요청 DTO
@JsonSerializable()
class ConnectionCreateRequest {
  final String provider;
  @JsonKey(name: 'auth_type')
  final String authType;
  @JsonKey(name: 'display_name')
  final String displayName;

  const ConnectionCreateRequest({
    required this.provider,
    required this.authType,
    required this.displayName,
  });

  factory ConnectionCreateRequest.fromJson(Map<String, dynamic> json) =>
      _$ConnectionCreateRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionCreateRequestToJson(this);

  /// Create from domain values
  factory ConnectionCreateRequest.create({
    required ConnectionProvider provider,
    required ConnectionAuthType authType,
    required String displayName,
  }) {
    return ConnectionCreateRequest(
      provider: provider.toServerValue(),
      authType: authType.toServerValue(),
      displayName: displayName,
    );
  }
}
