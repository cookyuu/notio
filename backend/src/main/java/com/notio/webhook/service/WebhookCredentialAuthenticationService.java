package com.notio.webhook.service;

import com.notio.auth.repository.UserRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionCredential;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import com.notio.connection.repository.ConnectionCredentialRepository;
import com.notio.connection.repository.ConnectionRepository;
import com.notio.connection.security.ApiKeyGenerator;
import com.notio.connection.security.ConnectionCredentialHasher;
import com.notio.webhook.dto.WebhookPrincipal;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookCredentialAuthenticationService {

    private static final Pattern API_KEY_PATTERN = Pattern.compile("^ntio_wh_([^_]+)_(.+)$");

    private final ConnectionCredentialRepository credentialRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final ConnectionCredentialHasher credentialHasher;

    public WebhookCredentialAuthenticationService(
        final ConnectionCredentialRepository credentialRepository,
        final ConnectionRepository connectionRepository,
        final UserRepository userRepository,
        final ConnectionCredentialHasher credentialHasher
    ) {
        this.credentialRepository = credentialRepository;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.credentialHasher = credentialHasher;
    }

    @Transactional
    public WebhookPrincipal authenticateApiKey(final HttpHeaders headers, final ConnectionProvider expectedProvider) {
        final String apiKey = extractApiKey(headers);
        final String prefix = extractPrefix(apiKey);
        final ConnectionCredential credential = credentialRepository
            .findActiveByKeyPrefixAndAuthType(prefix, ConnectionAuthType.API_KEY)
            .orElseThrow(() -> new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED));

        if (isExpired(credential) || !credentialHasher.matches(apiKey, credential.getKeyHash())) {
            throw new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        final Connection connection = connectionRepository
            .findByIdAndProviderAndStatusAndDeletedAtIsNull(credential.getConnectionId(), expectedProvider, ConnectionStatus.ACTIVE)
            .orElseThrow(() -> new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED));

        if (!userRepository.existsActiveById(connection.getUserId())) {
            throw new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        connection.refresh();
        return new WebhookPrincipal(connection.getId(), connection.getUserId(), connection.getProvider());
    }

    private String extractApiKey(final HttpHeaders headers) {
        final String explicit = headers.getFirst("X-Notio-Webhook-Key");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }

        final String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer " + ApiKeyGenerator.API_KEY_PREFIX + "_")) {
            return authorization.substring("Bearer ".length());
        }

        throw new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
    }

    private String extractPrefix(final String apiKey) {
        final var matcher = API_KEY_PATTERN.matcher(apiKey);
        if (!matcher.matches()) {
            throw new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }
        return matcher.group(1);
    }

    private boolean isExpired(final ConnectionCredential credential) {
        return credential.getExpiresAt() != null && !credential.getExpiresAt().isAfter(Instant.now());
    }
}
