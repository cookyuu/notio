import 'package:json_annotation/json_annotation.dart';

part 'connection_test_response.g.dart';

/// Connection 테스트 응답 DTO
@JsonSerializable()
class ConnectionTestResponse {
  final bool success;
  final String message;
  @JsonKey(name: 'tested_at')
  final String testedAt;

  const ConnectionTestResponse({
    required this.success,
    required this.message,
    required this.testedAt,
  });

  factory ConnectionTestResponse.fromJson(Map<String, dynamic> json) =>
      _$ConnectionTestResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionTestResponseToJson(this);

  /// Convert tested_at to DateTime
  DateTime get testedAtDateTime => DateTime.parse(testedAt);
}
