import 'package:json_annotation/json_annotation.dart';

part 'find_id_request.g.dart';

@JsonSerializable()
class FindIdRequest {
  final String email;

  const FindIdRequest({
    required this.email,
  });

  factory FindIdRequest.fromJson(Map<String, dynamic> json) =>
      _$FindIdRequestFromJson(json);

  Map<String, dynamic> toJson() => _$FindIdRequestToJson(this);
}
