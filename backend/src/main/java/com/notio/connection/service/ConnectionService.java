package com.notio.connection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectionService {

    private static final String API_KEY_PREFIX = "ntio_wh";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConnectionRepository connectionRepository;
    private final ConnectionCredentialRepository connectionCredentialRepository;
    private final ConnectionEventRepository connectionEventRepository;
    private final ObjectMapper objectMapper;

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
    public void recordWebhookFailure(final Connection connection, final String reason) {
        recordEvent(connection, "WEBHOOK_RECEIVED", "FAILURE", reasonMetadata(reason));
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
        final boolean supported = switch (provider) {
            case CLAUDE -> authType == ConnectionAuthType.API_KEY;
            case SLACK, GMAIL -> authType == ConnectionAuthType.OAUTH;
            case GITHUB, DISCORD, JIRA, LINEAR, TEAMS -> authType == ConnectionAuthType.SIGNATURE;
        };
        if (!supported) {
            throw new NotioException(ErrorCode.CONNECTION_AUTH_TYPE_UNSUPPORTED);
        }
    }

    private String createApiKeyCredential(final Connection connection) {
        final String prefix = randomToken(9);
        final String secret = randomToken(32);
        final String apiKey = API_KEY_PREFIX + "_" + prefix + "_" + secret;
        connectionCredentialRepository.save(ConnectionCredential.builder()
            .connectionId(connection.getId())
            .authType(ConnectionAuthType.API_KEY)
            .keyPrefix(prefix)
            .keyPreview(API_KEY_PREFIX + "_" + prefix + "_..." + secret.substring(secret.length() - 4))
            .keyHash(sha256(apiKey))
            .build());
        return apiKey;
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
        connectionEventRepository.save(ConnectionEvent.builder()
            .connectionId(connection.getId())
            .userId(connection.getUserId())
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

    private String randomToken(final int byteLength) {
        final byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
