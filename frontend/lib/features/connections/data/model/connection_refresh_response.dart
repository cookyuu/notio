import 'package:json_annotation/json_annotation.dart';

part 'connection_refresh_response.g.dart';

/// Connection 갱신 응답 DTO
@JsonSerializable()
class ConnectionRefreshResponse {
  final int id;
  final String status;
  @JsonKey(name: 'refreshed_at')
  final String refreshedAt;

  const ConnectionRefreshResponse({
    required this.id,
    required this.status,
    required this.refreshedAt,
  });

  factory ConnectionRefreshResponse.fromJson(Map<String, dynamic> json) =>
      _$ConnectionRefreshResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionRefreshResponseToJson(this);

  /// Convert refreshed_at to DateTime
  DateTime get refreshedAtDateTime => DateTime.parse(refreshedAt);
}
