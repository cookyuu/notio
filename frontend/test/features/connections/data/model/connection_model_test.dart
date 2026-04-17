import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/connections/data/model/connection_model.dart';
import 'package:notio_app/features/connections/domain/entity/connection_provider.dart';
import 'package:notio_app/features/connections/domain/entity/connection_auth_type.dart';
import 'package:notio_app/features/connections/domain/entity/connection_status.dart';
import 'package:notio_app/features/connections/domain/entity/connection_capability.dart';

void main() {
  group('ConnectionModel', () {
    group('fromJson', () {
      test('parses valid JSON with all fields', () {
        final json = {
          'id': 1,
          'provider': 'CLAUDE',
          'auth_type': 'API_KEY',
          'display_name': 'My Claude Connection',
          'account_label': 'test@example.com',
          'external_account_id': 'ext123',
          'external_workspace_id': 'workspace456',
          'subscription_id': 'sub789',
          'status': 'ACTIVE',
          'capabilities': ['WEBHOOK_RECEIVE', 'TEST_MESSAGE', 'ROTATE_KEY'],
          'metadata': {'key': 'value'},
          'key_preview': 'sk-...1234',
          'last_used_at': '2024-01-15T10:30:00Z',
          'created_at': '2024-01-01T00:00:00Z',
          'updated_at': '2024-01-10T00:00:00Z',
        };

        final model = ConnectionModel.fromJson(json);

        expect(model.id, 1);
        expect(model.provider, 'CLAUDE');
        expect(model.authType, 'API_KEY');
        expect(model.displayName, 'My Claude Connection');
        expect(model.accountLabel, 'test@example.com');
        expect(model.externalAccountId, 'ext123');
        expect(model.externalWorkspaceId, 'workspace456');
        expect(model.subscriptionId, 'sub789');
        expect(model.status, 'ACTIVE');
        expect(model.capabilities, ['WEBHOOK_RECEIVE', 'TEST_MESSAGE', 'ROTATE_KEY']);
        expect(model.metadata, {'key': 'value'});
        expect(model.keyPreview, 'sk-...1234');
        expect(model.lastUsedAt, '2024-01-15T10:30:00Z');
        expect(model.createdAt, '2024-01-01T00:00:00Z');
        expect(model.updatedAt, '2024-01-10T00:00:00Z');
      });

      test('parses JSON with minimal required fields', () {
        final json = {
          'id': 1,
          'provider': 'SLACK',
          'auth_type': 'OAUTH',
          'display_name': 'My Slack',
          'status': 'PENDING',
          'capabilities': [],
          'created_at': '2024-01-01T00:00:00Z',
          'updated_at': '2024-01-01T00:00:00Z',
        };

        final model = ConnectionModel.fromJson(json);

        expect(model.id, 1);
        expect(model.provider, 'SLACK');
        expect(model.authType, 'OAUTH');
        expect(model.displayName, 'My Slack');
        expect(model.status, 'PENDING');
        expect(model.capabilities, isEmpty);
        expect(model.accountLabel, isNull);
        expect(model.keyPreview, isNull);
        expect(model.lastUsedAt, isNull);
      });

      test('handles capabilities as JSON-encoded string', () {
        final json = {
          'id': 1,
          'provider': 'CLAUDE',
          'auth_type': 'API_KEY',
          'display_name': 'Test',
          'status': 'ACTIVE',
          'capabilities': '["WEBHOOK_RECEIVE","TEST_MESSAGE"]',
          'created_at': '2024-01-01T00:00:00Z',
          'updated_at': '2024-01-01T00:00:00Z',
        };

        final model = ConnectionModel.fromJson(json);

        expect(model.capabilities, ['WEBHOOK_RECEIVE', 'TEST_MESSAGE']);
      });

      test('handles metadata as JSON-encoded string', () {
        final json = {
          'id': 1,
          'provider': 'GITHUB',
          'auth_type': 'OAUTH',
          'display_name': 'Test',
          'status': 'ACTIVE',
          'capabilities': [],
          'metadata': '{"repo":"test/repo"}',
          'created_at': '2024-01-01T00:00:00Z',
          'updated_at': '2024-01-01T00:00:00Z',
        };

        final model = ConnectionModel.fromJson(json);

        expect(model.metadata, {'repo': 'test/repo'});
      });

      test('returns empty list for invalid capabilities JSON string', () {
        final json = {
          'id': 1,
          'provider': 'CLAUDE',
          'auth_type': 'API_KEY',
          'display_name': 'Test',
          'status': 'ACTIVE',
          'capabilities': 'invalid-json',
          'created_at': '2024-01-01T00:00:00Z',
          'updated_at': '2024-01-01T00:00:00Z',
        };

        final model = ConnectionModel.fromJson(json);

        expect(model.capabilities, isEmpty);
      });

      test('returns null for invalid metadata JSON string', () {
        final json = {
          'id': 1,
          'provider': 'CLAUDE',
          'auth_type': 'API_KEY',
          'display_name': 'Test',
          'status': 'ACTIVE',
          'capabilities': [],
          'metadata': 'invalid-json',
          'created_at': '2024-01-01T00:00:00Z',
          'updated_at': '2024-01-01T00:00:00Z',
        };

        final model = ConnectionModel.fromJson(json);

        expect(model.metadata, isNull);
      });
    });

    group('toEntity', () {
      test('converts model to entity correctly', () {
        const model = ConnectionModel(
          id: 1,
          provider: 'CLAUDE',
          authType: 'API_KEY',
          displayName: 'My Claude',
          accountLabel: 'test@example.com',
          externalAccountId: 'ext123',
          externalWorkspaceId: 'workspace456',
          subscriptionId: 'sub789',
          status: 'ACTIVE',
          capabilities: ['WEBHOOK_RECEIVE', 'TEST_MESSAGE'],
          metadata: {'key': 'value'},
          keyPreview: 'sk-...1234',
          lastUsedAt: '2024-01-15T10:30:00Z',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-10T00:00:00Z',
        );

        final entity = model.toEntity();

        expect(entity.id, 1);
        expect(entity.provider, ConnectionProvider.claude);
        expect(entity.authType, ConnectionAuthType.apiKey);
        expect(entity.displayName, 'My Claude');
        expect(entity.accountLabel, 'test@example.com');
        expect(entity.externalAccountId, 'ext123');
        expect(entity.externalWorkspaceId, 'workspace456');
        expect(entity.subscriptionId, 'sub789');
        expect(entity.status, ConnectionStatus.active);
        expect(entity.capabilities, [
          ConnectionCapability.webhookReceive,
          ConnectionCapability.testMessage
        ]);
        expect(entity.metadata, {'key': 'value'});
        expect(entity.keyPreview, 'sk-...1234');
        expect(entity.lastUsedAt, DateTime.parse('2024-01-15T10:30:00Z'));
        expect(entity.createdAt, DateTime.parse('2024-01-01T00:00:00Z'));
        expect(entity.updatedAt, DateTime.parse('2024-01-10T00:00:00Z'));
      });

      test('handles null optional fields', () {
        const model = ConnectionModel(
          id: 1,
          provider: 'SLACK',
          authType: 'OAUTH',
          displayName: 'My Slack',
          status: 'PENDING',
          capabilities: [],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        );

        final entity = model.toEntity();

        expect(entity.accountLabel, isNull);
        expect(entity.externalAccountId, isNull);
        expect(entity.externalWorkspaceId, isNull);
        expect(entity.subscriptionId, isNull);
        expect(entity.metadata, isNull);
        expect(entity.keyPreview, isNull);
        expect(entity.lastUsedAt, isNull);
      });
    });

    group('toJson', () {
      test('converts model to JSON correctly', () {
        const model = ConnectionModel(
          id: 1,
          provider: 'CLAUDE',
          authType: 'API_KEY',
          displayName: 'My Claude',
          status: 'ACTIVE',
          capabilities: ['WEBHOOK_RECEIVE'],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        );

        final json = model.toJson();

        expect(json['id'], 1);
        expect(json['provider'], 'CLAUDE');
        expect(json['auth_type'], 'API_KEY');
        expect(json['display_name'], 'My Claude');
        expect(json['status'], 'ACTIVE');
        expect(json['capabilities'], ['WEBHOOK_RECEIVE']);
        expect(json['created_at'], '2024-01-01T00:00:00Z');
        expect(json['updated_at'], '2024-01-01T00:00:00Z');
      });
    });
  });
}
