package com.notio.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.notio.auth.repository.UserRepository;
import com.notio.common.exception.NotioException;
import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionCredential;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import com.notio.connection.repository.ConnectionCredentialRepository;
import com.notio.connection.repository.ConnectionRepository;
import com.notio.connection.security.ConnectionCredentialHasher;
import com.notio.webhook.dto.WebhookPrincipal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class WebhookCredentialAuthenticationServiceTest {

    @Mock
    private ConnectionCredentialRepository credentialRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectionCredentialHasher credentialHasher;

    private WebhookCredentialAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new WebhookCredentialAuthenticationService(
            credentialRepository,
            connectionRepository,
            userRepository,
            credentialHasher
        );
    }

    @Test
    void authenticateApiKeyReturnsWebhookPrincipalWhenCredentialIsValid() {
        final String apiKey = "ntio_wh_prefix123_secret12345678901234567890123456789012";
        final ConnectionCredential credential = ConnectionCredential.builder()
            .connectionId(20L)
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix("prefix123")
            .keyHash("hash")
            .build();
        final Connection connection = Connection.builder()
            .id(20L)
            .userId(10L)
            .provider(ConnectionProvider.CLAUDE)
            .status(ConnectionStatus.ACTIVE)
            .build();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        when(credentialRepository.findActiveByKeyPrefixAndAuthType("prefix123", ConnectionAuthType.API_KEY))
            .thenReturn(Optional.of(credential));
        when(credentialHasher.matches(apiKey, "hash")).thenReturn(true);
        when(connectionRepository.findByIdAndProviderAndStatusAndDeletedAtIsNull(
            20L,
            ConnectionProvider.CLAUDE,
            ConnectionStatus.ACTIVE
        )).thenReturn(Optional.of(connection));
        when(userRepository.existsActiveById(10L)).thenReturn(true);

        final WebhookPrincipal principal = authenticationService.authenticateApiKey(headers, ConnectionProvider.CLAUDE);

        assertThat(principal.connectionId()).isEqualTo(20L);
        assertThat(principal.userId()).isEqualTo(10L);
        assertThat(principal.provider()).isEqualTo(ConnectionProvider.CLAUDE);
        assertThat(connection.getLastUsedAt()).isNotNull();
    }

    @Test
    void authenticateApiKeyRejectsExpiredCredential() {
        final String apiKey = "ntio_wh_prefix123_secret12345678901234567890123456789012";
        final ConnectionCredential credential = ConnectionCredential.builder()
            .connectionId(20L)
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix("prefix123")
            .keyHash("hash")
            .expiresAt(Instant.now().minusSeconds(1))
            .build();
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Notio-Webhook-Key", apiKey);

        when(credentialRepository.findActiveByKeyPrefixAndAuthType("prefix123", ConnectionAuthType.API_KEY))
            .thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> authenticationService.authenticateApiKey(headers, ConnectionProvider.CLAUDE))
            .isInstanceOf(NotioException.class)
            .hasMessage("Webhook 서명 검증에 실패했습니다.");
    }

    @Test
    void authenticateApiKeyRejectsMalformedCredential() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("malformed");

        assertThatThrownBy(() -> authenticationService.authenticateApiKey(headers, ConnectionProvider.CLAUDE))
            .isInstanceOf(NotioException.class)
            .hasMessage("Webhook 서명 검증에 실패했습니다.");
    }

    @Test
    void authenticateApiKeyRejectsRevokedOrMissingCredential() {
        final String apiKey = "ntio_wh_prefix123_secret12345678901234567890123456789012";
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Notio-Webhook-Key", apiKey);

        when(credentialRepository.findActiveByKeyPrefixAndAuthType("prefix123", ConnectionAuthType.API_KEY))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticateApiKey(headers, ConnectionProvider.CLAUDE))
            .isInstanceOf(NotioException.class)
            .hasMessage("Webhook 서명 검증에 실패했습니다.");
    }

    @Test
    void authenticateApiKeyRejectsProviderMismatch() {
        final String apiKey = "ntio_wh_prefix123_secret12345678901234567890123456789012";
        final ConnectionCredential credential = ConnectionCredential.builder()
            .connectionId(20L)
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix("prefix123")
            .keyHash("hash")
            .build();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        when(credentialRepository.findActiveByKeyPrefixAndAuthType("prefix123", ConnectionAuthType.API_KEY))
            .thenReturn(Optional.of(credential));
        when(credentialHasher.matches(apiKey, "hash")).thenReturn(true);
        when(connectionRepository.findByIdAndProviderAndStatusAndDeletedAtIsNull(
            20L,
            ConnectionProvider.SLACK,
            ConnectionStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticateApiKey(headers, ConnectionProvider.SLACK))
            .isInstanceOf(NotioException.class)
            .hasMessage("Webhook 서명 검증에 실패했습니다.");
    }
}
