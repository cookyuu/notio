package com.notio.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.NotioException;
import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionCredential;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import com.notio.connection.dto.ConnectionSecretResponse;
import com.notio.connection.dto.CreateConnectionRequest;
import com.notio.connection.repository.ConnectionCredentialRepository;
import com.notio.connection.repository.ConnectionEventRepository;
import com.notio.connection.repository.ConnectionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ConnectionService connectionService;

    @BeforeEach
    void setUp() {
        connectionService = new ConnectionService(
            connectionRepository,
            connectionCredentialRepository,
            connectionEventRepository,
            new ObjectMapper()
        );
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
        when(connectionCredentialRepository.save(any(ConnectionCredential.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final ConnectionSecretResponse response = connectionService.create(10L, request);

        assertThat(response.connection().id()).isEqualTo(20L);
        assertThat(response.apiKey()).startsWith("ntio_wh_");
        verify(connectionCredentialRepository).save(any(ConnectionCredential.class));
        verify(connectionEventRepository).save(any());
    }

    @Test
    void findByIdUsesUserScopedLookup() {
        when(connectionRepository.findByUserIdAndIdAndNotDeleted(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.findById(10L, 999L))
            .isInstanceOf(NotioException.class)
            .hasMessage("연결을 찾을 수 없습니다.");
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
}
