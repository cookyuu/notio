import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/connections/domain/entity/connection_provider.dart';
import 'package:notio_app/features/connections/domain/entity/connection_auth_type.dart';
import 'package:notio_app/features/connections/domain/entity/connection_status.dart';
import 'package:notio_app/features/connections/domain/entity/connection_capability.dart';

void main() {
  group('ConnectionProvider', () {
    test('fromServerValue converts correctly', () {
      expect(ConnectionProvider.fromServerValue('CLAUDE'), ConnectionProvider.claude);
      expect(ConnectionProvider.fromServerValue('SLACK'), ConnectionProvider.slack);
      expect(ConnectionProvider.fromServerValue('GMAIL'), ConnectionProvider.gmail);
      expect(ConnectionProvider.fromServerValue('GITHUB'), ConnectionProvider.github);
      expect(ConnectionProvider.fromServerValue('DISCORD'), ConnectionProvider.discord);
      expect(ConnectionProvider.fromServerValue('JIRA'), ConnectionProvider.jira);
      expect(ConnectionProvider.fromServerValue('LINEAR'), ConnectionProvider.linear);
      expect(ConnectionProvider.fromServerValue('TEAMS'), ConnectionProvider.teams);
    });

    test('toServerValue converts correctly', () {
      expect(ConnectionProvider.claude.toServerValue(), 'CLAUDE');
      expect(ConnectionProvider.slack.toServerValue(), 'SLACK');
      expect(ConnectionProvider.gmail.toServerValue(), 'GMAIL');
      expect(ConnectionProvider.github.toServerValue(), 'GITHUB');
      expect(ConnectionProvider.discord.toServerValue(), 'DISCORD');
      expect(ConnectionProvider.jira.toServerValue(), 'JIRA');
      expect(ConnectionProvider.linear.toServerValue(), 'LINEAR');
      expect(ConnectionProvider.teams.toServerValue(), 'TEAMS');
    });

    test('fromServerValue throws on unknown value', () {
      expect(
        () => ConnectionProvider.fromServerValue('UNKNOWN'),
        throwsArgumentError,
      );
    });
  });

  group('ConnectionAuthType', () {
    test('fromServerValue converts correctly', () {
      expect(ConnectionAuthType.fromServerValue('API_KEY'), ConnectionAuthType.apiKey);
      expect(ConnectionAuthType.fromServerValue('OAUTH'), ConnectionAuthType.oauth);
      expect(ConnectionAuthType.fromServerValue('SIGNATURE'), ConnectionAuthType.signature);
      expect(ConnectionAuthType.fromServerValue('SYSTEM'), ConnectionAuthType.system);
    });

    test('toServerValue converts correctly', () {
      expect(ConnectionAuthType.apiKey.toServerValue(), 'API_KEY');
      expect(ConnectionAuthType.oauth.toServerValue(), 'OAUTH');
      expect(ConnectionAuthType.signature.toServerValue(), 'SIGNATURE');
      expect(ConnectionAuthType.system.toServerValue(), 'SYSTEM');
    });

    test('fromServerValue throws on unknown value', () {
      expect(
        () => ConnectionAuthType.fromServerValue('UNKNOWN'),
        throwsArgumentError,
      );
    });
  });

  group('ConnectionStatus', () {
    test('fromServerValue converts correctly', () {
      expect(ConnectionStatus.fromServerValue('PENDING'), ConnectionStatus.pending);
      expect(ConnectionStatus.fromServerValue('ACTIVE'), ConnectionStatus.active);
      expect(ConnectionStatus.fromServerValue('NEEDS_ACTION'), ConnectionStatus.needsAction);
      expect(ConnectionStatus.fromServerValue('REVOKED'), ConnectionStatus.revoked);
      expect(ConnectionStatus.fromServerValue('ERROR'), ConnectionStatus.error);
    });

    test('toServerValue converts correctly', () {
      expect(ConnectionStatus.pending.toServerValue(), 'PENDING');
      expect(ConnectionStatus.active.toServerValue(), 'ACTIVE');
      expect(ConnectionStatus.needsAction.toServerValue(), 'NEEDS_ACTION');
      expect(ConnectionStatus.revoked.toServerValue(), 'REVOKED');
      expect(ConnectionStatus.error.toServerValue(), 'ERROR');
    });

    test('fromServerValue throws on unknown value', () {
      expect(
        () => ConnectionStatus.fromServerValue('UNKNOWN'),
        throwsArgumentError,
      );
    });
  });

  group('ConnectionCapability', () {
    test('fromServerValue converts correctly', () {
      expect(ConnectionCapability.fromServerValue('WEBHOOK_RECEIVE'),
             ConnectionCapability.webhookReceive);
      expect(ConnectionCapability.fromServerValue('TEST_MESSAGE'),
             ConnectionCapability.testMessage);
      expect(ConnectionCapability.fromServerValue('REFRESH_TOKEN'),
             ConnectionCapability.refreshToken);
      expect(ConnectionCapability.fromServerValue('ROTATE_KEY'),
             ConnectionCapability.rotateKey);
    });

    test('toServerValue converts correctly', () {
      expect(ConnectionCapability.webhookReceive.toServerValue(), 'WEBHOOK_RECEIVE');
      expect(ConnectionCapability.testMessage.toServerValue(), 'TEST_MESSAGE');
      expect(ConnectionCapability.refreshToken.toServerValue(), 'REFRESH_TOKEN');
      expect(ConnectionCapability.rotateKey.toServerValue(), 'ROTATE_KEY');
    });

    test('fromServerValues converts array correctly', () {
      final result = ConnectionCapability.fromServerValues([
        'WEBHOOK_RECEIVE',
        'TEST_MESSAGE',
        'ROTATE_KEY'
      ]);

      expect(result, [
        ConnectionCapability.webhookReceive,
        ConnectionCapability.testMessage,
        ConnectionCapability.rotateKey
      ]);
    });

    test('toServerValues converts array correctly', () {
      final result = ConnectionCapability.toServerValues([
        ConnectionCapability.webhookReceive,
        ConnectionCapability.testMessage,
        ConnectionCapability.rotateKey
      ]);

      expect(result, ['WEBHOOK_RECEIVE', 'TEST_MESSAGE', 'ROTATE_KEY']);
    });

    test('fromServerValue throws on unknown value', () {
      expect(
        () => ConnectionCapability.fromServerValue('UNKNOWN'),
        throwsArgumentError,
      );
    });
  });
}
