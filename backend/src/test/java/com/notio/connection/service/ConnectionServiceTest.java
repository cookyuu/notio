package com.notio.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.NotioException;
import com.notio.connection.adapter.ConnectionProviderAdapter;
import com.notio.connection.adapter.ConnectionProviderAdapterRegistry;
import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionCredential;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import com.notio.connection.dto.ConnectionResponse;
import com.notio.connection.dto.ConnectionSecretResponse;
import com.notio.connection.dto.CreateConnectionRequest;
import com.notio.connection.repository.ConnectionCredentialRepository;
import com.notio.connection.repository.ConnectionEventRepository;
import com.notio.connection.repository.ConnectionRepository;
import com.notio.connection.security.ApiKeyGenerator;
import com.notio.connection.security.ConnectionCredentialHasher;
import com.notio.connection.security.CredentialEncryptionService;
import com.notio.connection.security.GeneratedApiKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private ConnectionCredentialRepository connectionCredentialRepository;

    @Mock
    private ConnectionEventRepository connectionEventRepository;

    @Mock
    private ConnectionProviderAdapterRegistry adapterRegistry;

    @Mock
    private ConnectionProviderAdapter connectionProviderAdapter;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private ConnectionCredentialHasher credentialHasher;

    @Mock
    private CredentialEncryptionService credentialEncryptionService;

    private ConnectionService connectionService;

    @BeforeEach
    void setUp() {
        connectionService = new ConnectionService(
            connectionRepository,
            connectionCredentialRepository,
            connectionEventRepository,
            new ObjectMapper(),
            adapterRegistry,
            apiKeyGenerator,
            credentialHasher,
            credentialEncryptionService
        );
        lenient().when(adapterRegistry.get(ConnectionProvider.CLAUDE)).thenReturn(connectionProviderAdapter);
        lenient().when(adapterRegistry.get(ConnectionProvider.SLACK)).thenReturn(connectionProviderAdapter);
    }

    @Test
    void createClaudeConnectionReturnsApiKeyOnce() {
        final CreateConnectionRequest request = new CreateConnectionRequest(
            ConnectionProvider.CLAUDE,
            ConnectionAuthType.API_KEY,
            "Claude Code",
            "local",
            null,
            null,
            null
        );
        final Connection savedConnection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .authType(ConnectionAuthType.API_KEY)
            .displayName("Claude Code")
            .status(ConnectionStatus.ACTIVE)
            .capabilities("[]")
            .metadata("{}")
            .build();
        when(connectionRepository.save(any(Connection.class))).thenReturn(savedConnection);
        when(connectionProviderAdapter.supportsAuthType(ConnectionAuthType.API_KEY)).thenReturn(true);
        when(apiKeyGenerator.generate())
            .thenReturn(new GeneratedApiKey("ntio_wh_prefix123_secret12345678901234567890123456789012", "prefix123", "ntio_wh_prefix123_...9012"));
        when(credentialHasher.hash("ntio_wh_prefix123_secret12345678901234567890123456789012")).thenReturn("hash");
        when(connectionCredentialRepository.save(any(ConnectionCredential.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final ConnectionSecretResponse response = connectionService.create(10L, request);

        assertThat(response.connection().id()).isEqualTo(20L);
        assertThat(response.apiKey()).startsWith("ntio_wh_");
        assertThat(response.connection()).extracting(ConnectionResponse::metadata, ConnectionResponse::capabilities)
            .containsExactly("{}", "[]");
        verify(connectionCredentialRepository).save(any(ConnectionCredential.class));
        verify(connectionEventRepository).save(any());
    }

    @Test
    void createStoresOnlyHashedApiKeyMaterial() {
        final CreateConnectionRequest request = new CreateConnectionRequest(
            ConnectionProvider.CLAUDE,
            ConnectionAuthType.API_KEY,
            "Claude Code",
            null,
            null,
            null,
            null
        );
        final Connection savedConnection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .authType(ConnectionAuthType.API_KEY)
            .displayName("Claude Code")
            .status(ConnectionStatus.ACTIVE)
            .capabilities("[]")
            .metadata("{}")
            .build();
        when(connectionRepository.save(any(Connection.class))).thenReturn(savedConnection);
        when(connectionProviderAdapter.supportsAuthType(ConnectionAuthType.API_KEY)).thenReturn(true);
        when(apiKeyGenerator.generate()).thenReturn(
            new GeneratedApiKey(
                "ntio_wh_prefix123_secret12345678901234567890123456789012",
                "prefix123",
                "ntio_wh_prefix123_...9012"
            )
        );
        when(credentialHasher.hash("ntio_wh_prefix123_secret12345678901234567890123456789012")).thenReturn("hashed-key");
        when(connectionCredentialRepository.save(any(ConnectionCredential.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final ConnectionSecretResponse response = connectionService.create(10L, request);
        final ArgumentCaptor<ConnectionCredential> credentialCaptor = ArgumentCaptor.forClass(ConnectionCredential.class);

        verify(connectionCredentialRepository).save(credentialCaptor.capture());
        assertThat(response.apiKey()).isEqualTo("ntio_wh_prefix123_secret12345678901234567890123456789012");
        assertThat(credentialCaptor.getValue().getKeyPrefix()).isEqualTo("prefix123");
        assertThat(credentialCaptor.getValue().getKeyPreview()).isEqualTo("ntio_wh_prefix123_...9012");
        assertThat(credentialCaptor.getValue().getKeyHash()).isEqualTo("hashed-key");
        assertThat(credentialCaptor.getValue().getAccessTokenEncrypted()).isNull();
        assertThat(credentialCaptor.getValue().getRefreshTokenEncrypted()).isNull();
    }

    @Test
    void findAllReturnsOnlyOwnedConnectionsWithoutApiKey() {
        final Connection connection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .authType(ConnectionAuthType.API_KEY)
            .displayName("Claude Code")
            .status(ConnectionStatus.ACTIVE)
            .capabilities("[]")
            .metadata("{}")
            .build();
        when(connectionRepository.findAllByUserIdAndNotDeleted(10L)).thenReturn(List.of(connection));

        final List<ConnectionResponse> responses = connectionService.findAll(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(20L);
        assertThat(responses.getFirst().metadata()).isEqualTo("{}");
        assertThat(responses.getFirst().capabilities()).isEqualTo("[]");
    }

    @Test
    void findByIdUsesUserScopedLookup() {
        when(connectionRepository.findByUserIdAndIdAndNotDeleted(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.findById(10L, 999L))
            .isInstanceOf(NotioException.class)
            .hasMessage("연결을 찾을 수 없습니다.");
    }

    @Test
    void findByIdReturnsConnectionWithoutApiKeyExposure() {
        final Connection connection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .authType(ConnectionAuthType.API_KEY)
            .displayName("Claude Code")
            .status(ConnectionStatus.ACTIVE)
            .capabilities("[]")
            .metadata("{}")
            .build();
        when(connectionRepository.findByUserIdAndIdAndNotDeleted(10L, 20L)).thenReturn(Optional.of(connection));

        final ConnectionResponse response = connectionService.findById(10L, 20L);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.metadata()).isEqualTo("{}");
        assertThat(response.capabilities()).isEqualTo("[]");
    }

    @Test
    void rotateKeyReturnsNewApiKeyOnlyInSecretResponse() {
        final Connection connection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .authType(ConnectionAuthType.API_KEY)
            .displayName("Claude Code")
            .status(ConnectionStatus.ACTIVE)
            .capabilities("[]")
            .metadata("{}")
            .build();
        final ConnectionCredential activeCredential = ConnectionCredential.builder()
            .connectionId(20L)
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix("oldprefix")
            .keyHash("oldhash")
            .build();
        when(connectionRepository.findByUserIdAndIdAndNotDeleted(10L, 20L)).thenReturn(Optional.of(connection));
        when(connectionCredentialRepository.findActiveByConnectionIdAndAuthType(20L, ConnectionAuthType.API_KEY))
            .thenReturn(Optional.of(activeCredential));
        when(apiKeyGenerator.generate()).thenReturn(
            new GeneratedApiKey(
                "ntio_wh_prefix999_secret99999999999999999999999999999999",
                "prefix999",
                "ntio_wh_prefix999_...9999"
            )
        );
        when(credentialHasher.hash("ntio_wh_prefix999_secret99999999999999999999999999999999")).thenReturn("newhash");
        when(connectionCredentialRepository.save(any(ConnectionCredential.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final ConnectionSecretResponse response = connectionService.rotateKey(10L, 20L);

        assertThat(response.apiKey()).startsWith("ntio_wh_prefix999_");
        assertThat(activeCredential.getRevokedAt()).isNotNull();
        assertThat(response.connection().metadata()).isEqualTo("{}");
        assertThat(response.connection().capabilities()).isEqualTo("[]");
    }

    @Test
    void rotateKeyRejectsNonApiKeyConnection() {
        final Connection connection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.SLACK)
            .authType(ConnectionAuthType.OAUTH)
            .displayName("Slack")
            .status(ConnectionStatus.PENDING)
            .capabilities("[]")
            .metadata("{}")
            .build();
        when(connectionRepository.findByUserIdAndIdAndNotDeleted(10L, 20L))
            .thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> connectionService.rotateKey(10L, 20L))
            .isInstanceOf(NotioException.class)
            .hasMessage("지원하지 않는 연결 인증 방식입니다.");
    }

    @Test
    void deleteRevokesCredentialsAndConnection() {
        final Connection connection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .authType(ConnectionAuthType.API_KEY)
            .displayName("Claude Code")
            .status(ConnectionStatus.ACTIVE)
            .capabilities("[]")
            .metadata("{}")
            .build();
        final ConnectionCredential credential = ConnectionCredential.builder()
            .connectionId(20L)
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix("prefix123")
            .keyHash("hash")
            .build();
        when(connectionRepository.findByUserIdAndIdAndNotDeleted(10L, 20L)).thenReturn(Optional.of(connection));
        when(connectionCredentialRepository.findAllActiveByConnectionId(20L)).thenReturn(List.of(credential));

        connectionService.delete(10L, 20L);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.REVOKED);
        assertThat(connection.getDeletedAt()).isNotNull();
        assertThat(credential.getRevokedAt()).isNotNull();
        verify(connectionEventRepository).save(any());
    }

    @Test
    void recordWebhookFailurePersistsAuditEvent() {
        connectionService.recordWebhookFailure(10L, 20L, "invalid signature");

        verify(connectionEventRepository).save(any());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void recordWebhookSuccessPersistsAuditEvent() {
        connectionService.recordWebhookSuccess(10L, 20L);

        verify(connectionEventRepository).save(any());
        verify(connectionRepository, never()).save(any());
    }
}
