package com.notio.connection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.connection.adapter.ConnectionProviderAdapter;
import com.notio.connection.adapter.ConnectionProviderAdapterRegistry;
import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionCapability;
import com.notio.connection.domain.ConnectionCredential;
import com.notio.connection.domain.ConnectionEvent;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import com.notio.connection.dto.ConnectionActionResponse;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ConnectionCredentialRepository connectionCredentialRepository;
    private final ConnectionEventRepository connectionEventRepository;
    private final ObjectMapper objectMapper;
    private final ConnectionProviderAdapterRegistry adapterRegistry;
    private final ApiKeyGenerator apiKeyGenerator;
    private final ConnectionCredentialHasher credentialHasher;
    private final CredentialEncryptionService credentialEncryptionService;

    public List<ConnectionResponse> findAll(final Long userId) {
        return connectionRepository.findAllByUserIdAndNotDeleted(userId)
            .stream()
            .map(ConnectionResponse::from)
            .toList();
    }

    public ConnectionResponse findById(final Long userId, final Long id) {
        return ConnectionResponse.from(findOwnedConnection(userId, id));
    }

    @Transactional
    public ConnectionSecretResponse create(final Long userId, final CreateConnectionRequest request) {
        final ConnectionAuthType authType = request.authType() == null ? defaultAuthType(request.provider()) : request.authType();
        validateSupported(request.provider(), authType);

        final Connection connection = connectionRepository.save(Connection.builder()
            .userId(userId)
            .provider(request.provider())
            .authType(authType)
            .displayName(request.displayName())
            .accountLabel(request.accountLabel())
            .externalAccountId(request.externalAccountId())
            .externalWorkspaceId(request.externalWorkspaceId())
            .subscriptionId(request.subscriptionId())
            .status(initialStatus(authType))
            .capabilities(capabilitiesJson(authType))
            .metadata("{}")
            .build());

        final String apiKey = authType == ConnectionAuthType.API_KEY ? createApiKeyCredential(connection) : null;
        recordEvent(connection, "CONNECTION_CREATED", "SUCCESS", Map.of("provider", request.provider().name()));
        return new ConnectionSecretResponse(ConnectionResponse.from(connection), apiKey);
    }

    @Transactional
    public void delete(final Long userId, final Long id) {
        final Connection connection = findOwnedConnection(userId, id);
        connectionCredentialRepository.findAllActiveByConnectionId(connection.getId())
            .forEach(ConnectionCredential::revoke);
        connection.revoke();
        recordEvent(connection, "API_KEY_REVOKED", "SUCCESS", Map.of());
    }

    @Transactional
    public ConnectionActionResponse test(final Long userId, final Long id) {
        final Connection connection = findOwnedConnection(userId, id);
        connection.refresh();
        recordEvent(connection, "CONNECTION_TEST", "SUCCESS", Map.of("provider", connection.getProvider().name()));
        return new ConnectionActionResponse(connection.getId(), "SUCCESS", "연결 테스트 이벤트가 기록되었습니다.");
    }

    @Transactional
    public ConnectionActionResponse refresh(final Long userId, final Long id) {
        final Connection connection = findOwnedConnection(userId, id);
        connection.refresh();
        recordEvent(connection, "PROVIDER_TOKEN_REFRESH", "SUCCESS", Map.of("authType", connection.getAuthType().name()));
        return new ConnectionActionResponse(connection.getId(), "SUCCESS", "연결 갱신 이벤트가 기록되었습니다.");
    }

    @Transactional
    public ConnectionSecretResponse rotateKey(final Long userId, final Long id) {
        final Connection connection = findOwnedConnection(userId, id);
        if (connection.getAuthType() != ConnectionAuthType.API_KEY) {
            throw new NotioException(ErrorCode.CONNECTION_AUTH_TYPE_UNSUPPORTED);
        }

        connectionCredentialRepository.findActiveByConnectionIdAndAuthType(connection.getId(), ConnectionAuthType.API_KEY)
            .ifPresent(ConnectionCredential::revoke);
        final String apiKey = createApiKeyCredential(connection);
        recordEvent(connection, "API_KEY_ROTATED", "SUCCESS", Map.of());
        return new ConnectionSecretResponse(ConnectionResponse.from(connection), apiKey);
    }

    @Transactional
    public void recordOAuthStarted(final Long userId, final Long id) {
        recordEvent(findOwnedConnection(userId, id), "OAUTH_STARTED", "SUCCESS", Map.of());
    }

    @Transactional
    public void recordOAuthSucceeded(final Long userId, final Long id) {
        final Connection connection = findOwnedConnection(userId, id);
        connection.activate();
        recordEvent(connection, "OAUTH_SUCCEEDED", "SUCCESS", Map.of());
    }

    @Transactional
    public void recordOAuthFailed(final Long userId, final Long id, final String reason) {
        recordEvent(findOwnedConnection(userId, id), "OAUTH_FAILED", "FAILURE", reasonMetadata(reason));
    }

    @Transactional
    public void recordWebhookSuccess(final Connection connection) {
        recordEvent(connection, "WEBHOOK_RECEIVED", "SUCCESS", Map.of());
    }

    @Transactional
    public void recordWebhookSuccess(final Long userId, final Long connectionId) {
        recordEvent(userId, connectionId, "WEBHOOK_RECEIVED", "SUCCESS", Map.of());
    }

    @Transactional
    public void recordWebhookFailure(final Connection connection, final String reason) {
        recordEvent(connection, "WEBHOOK_RECEIVED", "FAILURE", reasonMetadata(reason));
    }

    @Transactional
    public void recordWebhookFailure(final Long userId, final Long connectionId, final String reason) {
        recordEvent(userId, connectionId, "WEBHOOK_RECEIVED", "FAILURE", reasonMetadata(reason));
    }

    @Transactional
    public void recordRateLimitHit(final Connection connection) {
        recordEvent(connection, "RATE_LIMIT_HIT", "FAILURE", Map.of());
    }

    @Transactional
    public void recordProviderTokenRefreshFailed(final Long userId, final Long id, final String reason) {
        recordEvent(findOwnedConnection(userId, id), "PROVIDER_TOKEN_REFRESH", "FAILURE", reasonMetadata(reason));
    }

    private Connection findOwnedConnection(final Long userId, final Long id) {
        return connectionRepository.findByUserIdAndIdAndNotDeleted(userId, id)
            .orElseThrow(() -> new NotioException(ErrorCode.CONNECTION_NOT_FOUND));
    }

    private ConnectionAuthType defaultAuthType(final ConnectionProvider provider) {
        return switch (provider) {
            case CLAUDE -> ConnectionAuthType.API_KEY;
            case SLACK, GMAIL -> ConnectionAuthType.OAUTH;
            case GITHUB, DISCORD, JIRA, LINEAR, TEAMS -> ConnectionAuthType.SIGNATURE;
        };
    }

    private ConnectionStatus initialStatus(final ConnectionAuthType authType) {
        return authType == ConnectionAuthType.API_KEY ? ConnectionStatus.ACTIVE : ConnectionStatus.PENDING;
    }

    private void validateSupported(final ConnectionProvider provider, final ConnectionAuthType authType) {
        final ConnectionProviderAdapter adapter = adapterRegistry.get(provider);
        if (!adapter.supportsAuthType(authType)) {
            throw new NotioException(ErrorCode.CONNECTION_AUTH_TYPE_UNSUPPORTED);
        }
    }

    private String createApiKeyCredential(final Connection connection) {
        final GeneratedApiKey apiKey = apiKeyGenerator.generate();
        connectionCredentialRepository.save(ConnectionCredential.builder()
            .connectionId(connection.getId())
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix(apiKey.prefix())
            .keyPreview(apiKey.preview())
            .keyHash(credentialHasher.hash(apiKey.value()))
            .build());
        return apiKey.value();
    }

    @Transactional
    public void saveOAuthTokens(
        final Connection connection,
        final String accessToken,
        final String refreshToken,
        final Instant expiresAt
    ) {
        connectionCredentialRepository.save(ConnectionCredential.builder()
            .connectionId(connection.getId())
            .authType(ConnectionAuthType.OAUTH)
            .accessTokenEncrypted(credentialEncryptionService.encrypt(accessToken))
            .refreshTokenEncrypted(credentialEncryptionService.encrypt(refreshToken))
            .expiresAt(expiresAt)
            .build());
    }

    private String capabilitiesJson(final ConnectionAuthType authType) {
        final List<ConnectionCapability> capabilities = authType == ConnectionAuthType.API_KEY
            ? List.of(ConnectionCapability.WEBHOOK_RECEIVE, ConnectionCapability.TEST_MESSAGE, ConnectionCapability.ROTATE_KEY)
            : List.of(ConnectionCapability.WEBHOOK_RECEIVE, ConnectionCapability.TEST_MESSAGE, ConnectionCapability.REFRESH_TOKEN);
        try {
            return objectMapper.writeValueAsString(capabilities);
        } catch (JsonProcessingException exception) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void recordEvent(
        final Connection connection,
        final String eventType,
        final String status,
        final Map<String, Object> metadata
    ) {
        recordEvent(connection.getUserId(), connection.getId(), eventType, status, metadata);
    }

    private void recordEvent(
        final Long userId,
        final Long connectionId,
        final String eventType,
        final String status,
        final Map<String, Object> metadata
    ) {
        connectionEventRepository.save(ConnectionEvent.builder()
            .connectionId(connectionId)
            .userId(userId)
            .eventType(eventType)
            .status(status)
            .metadata(metadataJson(metadata))
            .build());
    }

    private Map<String, Object> reasonMetadata(final String reason) {
        if (reason == null || reason.isBlank()) {
            return Map.of();
        }
        return Map.of("reason", reason);
    }

    private String metadataJson(final Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

}
