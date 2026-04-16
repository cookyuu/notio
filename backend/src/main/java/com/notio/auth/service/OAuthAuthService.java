package com.notio.auth.service;

import com.notio.auth.adapter.AuthProviderAdapter;
import com.notio.auth.adapter.AuthProviderAdapterRegistry;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.AuthProviderState;
import com.notio.auth.dto.OAuthCallbackResponse;
import com.notio.auth.dto.OAuthExchangeRequest;
import com.notio.auth.dto.OAuthExchangeResponse;
import com.notio.auth.dto.OAuthStartRequest;
import com.notio.auth.dto.OAuthStartResponse;
import com.notio.auth.repository.AuthProviderStateRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAuthService {

    private static final long OAUTH_STATE_TTL_MINUTES = 5L;

    private final AuthProviderAdapterRegistry authProviderAdapterRegistry;
    private final AuthProviderStateRepository authProviderStateRepository;

    @Transactional
    public OAuthStartResponse start(final OAuthStartRequest request) {
        final AuthProviderAdapter adapter = authProviderAdapterRegistry.get(request.getProvider());
        final OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(OAUTH_STATE_TTL_MINUTES);
        final AuthProviderState providerState = authProviderStateRepository.save(AuthProviderState.builder()
                .provider(request.getProvider())
                .state(UUID.randomUUID().toString())
                .platform(request.getPlatform())
                .redirectUri(request.getRedirectUri().trim())
                .pkceVerifier(normalizePkceVerifier(request.getPkceVerifier()))
                .expiresAt(expiresAt)
                .build());

        final String authorizationUrl = adapter.buildAuthorizationUrl(providerState);
        log.info("OAuth start initialized: provider={}, state={}", request.getProvider(), providerState.getState());

        return OAuthStartResponse.builder()
                .provider(providerState.getProvider())
                .platform(providerState.getPlatform())
                .state(providerState.getState())
                .authorizationUrl(authorizationUrl)
                .expiresAt(providerState.getExpiresAt())
                .build();
    }

    public OAuthCallbackResponse callback(
            final String providerValue,
            final String state,
            final String code,
            final String error
    ) {
        final AuthProvider provider = resolveProvider(providerValue);
        final AuthProviderState providerState = validateState(provider, state);

        if (error != null && !error.isBlank()) {
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED, "OAuth provider returned error: " + error);
        }
        if (code == null || code.isBlank()) {
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED, "OAuth authorization code is missing.");
        }

        final AuthProviderAdapter adapter = authProviderAdapterRegistry.get(provider);
        try {
            return adapter.handleCallback(code.trim(), providerState);
        } catch (NotioException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED);
        }
    }

    public OAuthExchangeResponse exchange(final OAuthExchangeRequest request) {
        final AuthProviderState providerState = validateState(request.getProvider(), request.getState());
        final AuthProviderAdapter adapter = authProviderAdapterRegistry.get(request.getProvider());

        try {
            return adapter.exchangeAuthorizationCode(request.getCode().trim(), providerState);
        } catch (NotioException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED);
        }
    }

    private AuthProviderState validateState(final AuthProvider provider, final String stateValue) {
        final String normalizedState = stateValue == null ? "" : stateValue.trim();
        final AuthProviderState providerState = authProviderStateRepository.findByState(normalizedState)
                .orElseThrow(() -> new NotioException(ErrorCode.OAUTH_STATE_INVALID));

        if (providerState.getProvider() != provider || providerState.isExpired()) {
            throw new NotioException(ErrorCode.OAUTH_STATE_INVALID);
        }

        return providerState;
    }

    private AuthProvider resolveProvider(final String providerValue) {
        try {
            return AuthProvider.valueOf(providerValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new NotioException(ErrorCode.AUTH_PROVIDER_UNSUPPORTED);
        }
    }

    private String normalizePkceVerifier(final String pkceVerifier) {
        if (pkceVerifier == null || pkceVerifier.isBlank()) {
            return null;
        }
        return pkceVerifier.trim();
    }
}
