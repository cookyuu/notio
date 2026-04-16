import 'dart:convert';
import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/connection_entity.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../../domain/entity/connection_status.dart';
import '../../domain/entity/connection_capability.dart';

part 'connection_model.g.dart';

/// Custom converter for capabilities field
/// Handles both List of String and String (JSON-encoded array)
class CapabilitiesConverter implements JsonConverter<List<String>, dynamic> {
  const CapabilitiesConverter();

  @override
  List<String> fromJson(dynamic json) {
    if (json is List) {
      return json.map((e) => e.toString()).toList();
    } else if (json is String) {
      // If server sends JSON-encoded string, parse it
      try {
        final decoded = jsonDecode(json);
        if (decoded is List) {
          return decoded.map((e) => e.toString()).toList();
        }
      } catch (e) {
        // If parsing fails, return empty list
        return [];
      }
    }
    return [];
  }

  @override
  dynamic toJson(List<String> object) => object;
}

/// Custom converter for metadata field
/// Handles both Map and String (JSON-encoded object)
class MetadataConverter implements JsonConverter<Map<String, dynamic>?, dynamic> {
  const MetadataConverter();

  @override
  Map<String, dynamic>? fromJson(dynamic json) {
    if (json == null) {
      return null;
    }
    if (json is Map<String, dynamic>) {
      return json;
    } else if (json is String) {
      // If server sends JSON-encoded string, parse it
      try {
        final decoded = jsonDecode(json);
        if (decoded is Map) {
          return Map<String, dynamic>.from(decoded);
        }
      } catch (e) {
        // If parsing fails, return null
        return null;
      }
    }
    return null;
  }

  @override
  dynamic toJson(Map<String, dynamic>? object) => object;
}

/// Connection data model for JSON serialization
@JsonSerializable()
class ConnectionModel {
  final int id;
  final String provider;
  @JsonKey(name: 'auth_type')
  final String authType;
  @JsonKey(name: 'display_name')
  final String displayName;
  @JsonKey(name: 'account_label')
  final String? accountLabel;
  @JsonKey(name: 'external_account_id')
  final String? externalAccountId;
  @JsonKey(name: 'external_workspace_id')
  final String? externalWorkspaceId;
  @JsonKey(name: 'subscription_id')
  final String? subscriptionId;
  final String status;
  @CapabilitiesConverter()
  final List<String> capabilities;
  @MetadataConverter()
  final Map<String, dynamic>? metadata;
  @JsonKey(name: 'key_preview')
  final String? keyPreview;
  @JsonKey(name: 'last_used_at')
  final String? lastUsedAt;
  @JsonKey(name: 'created_at')
  final String createdAt;
  @JsonKey(name: 'updated_at')
  final String updatedAt;

  const ConnectionModel({
    required this.id,
    required this.provider,
    required this.authType,
    required this.displayName,
    required this.status,
    required this.capabilities,
    required this.createdAt,
    required this.updatedAt,
    this.accountLabel,
    this.externalAccountId,
    this.externalWorkspaceId,
    this.subscriptionId,
    this.metadata,
    this.keyPreview,
    this.lastUsedAt,
  });

  factory ConnectionModel.fromJson(Map<String, dynamic> json) =>
      _$ConnectionModelFromJson(json);

  Map<String, dynamic> toJson() => _$ConnectionModelToJson(this);

  /// Convert to domain entity
  ConnectionEntity toEntity() {
    return ConnectionEntity(
      id: id,
      provider: ConnectionProvider.fromServerValue(provider),
      authType: ConnectionAuthType.fromServerValue(authType),
      displayName: displayName,
      accountLabel: accountLabel,
      externalAccountId: externalAccountId,
      externalWorkspaceId: externalWorkspaceId,
      subscriptionId: subscriptionId,
      status: ConnectionStatus.fromServerValue(status),
      capabilities: ConnectionCapability.fromServerValues(capabilities),
      metadata: metadata,
      keyPreview: keyPreview,
      lastUsedAt: lastUsedAt != null ? DateTime.parse(lastUsedAt!) : null,
      createdAt: DateTime.parse(createdAt),
      updatedAt: DateTime.parse(updatedAt),
    );
  }

  /// Convert from domain entity
  factory ConnectionModel.fromEntity(ConnectionEntity entity) {
    return ConnectionModel(
      id: entity.id,
      provider: entity.provider.toServerValue(),
      authType: entity.authType.toServerValue(),
      displayName: entity.displayName,
      accountLabel: entity.accountLabel,
      externalAccountId: entity.externalAccountId,
      externalWorkspaceId: entity.externalWorkspaceId,
      subscriptionId: entity.subscriptionId,
      status: entity.status.toServerValue(),
      capabilities: ConnectionCapability.toServerValues(entity.capabilities),
      metadata: entity.metadata,
      keyPreview: entity.keyPreview,
      lastUsedAt: entity.lastUsedAt?.toIso8601String(),
      createdAt: entity.createdAt.toIso8601String(),
      updatedAt: entity.updatedAt.toIso8601String(),
    );
  }
}
