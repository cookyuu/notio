import 'connection_provider.dart';
import 'connection_auth_type.dart';
import 'connection_status.dart';
import 'connection_capability.dart';

/// Connection domain entity
class ConnectionEntity {
  final int id;
  final ConnectionProvider provider;
  final ConnectionAuthType authType;
  final String displayName;
  final String? accountLabel;
  final String? externalAccountId;
  final String? externalWorkspaceId;
  final String? subscriptionId;
  final ConnectionStatus status;
  final List<ConnectionCapability> capabilities;
  final Map<String, dynamic>? metadata;
  final String? keyPreview;
  final DateTime? lastUsedAt;
  final DateTime createdAt;
  final DateTime updatedAt;

  const ConnectionEntity({
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

  ConnectionEntity copyWith({
    int? id,
    ConnectionProvider? provider,
    ConnectionAuthType? authType,
    String? displayName,
    String? accountLabel,
    String? externalAccountId,
    String? externalWorkspaceId,
    String? subscriptionId,
    ConnectionStatus? status,
    List<ConnectionCapability>? capabilities,
    Map<String, dynamic>? metadata,
    String? keyPreview,
    DateTime? lastUsedAt,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return ConnectionEntity(
      id: id ?? this.id,
      provider: provider ?? this.provider,
      authType: authType ?? this.authType,
      displayName: displayName ?? this.displayName,
      accountLabel: accountLabel ?? this.accountLabel,
      externalAccountId: externalAccountId ?? this.externalAccountId,
      externalWorkspaceId: externalWorkspaceId ?? this.externalWorkspaceId,
      subscriptionId: subscriptionId ?? this.subscriptionId,
      status: status ?? this.status,
      capabilities: capabilities ?? this.capabilities,
      metadata: metadata ?? this.metadata,
      keyPreview: keyPreview ?? this.keyPreview,
      lastUsedAt: lastUsedAt ?? this.lastUsedAt,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
