import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/connection_entity.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../../domain/entity/connection_status.dart';
import '../../domain/entity/connection_capability.dart';

part 'connection_model.g.dart';

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
  final List<String> capabilities;
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
