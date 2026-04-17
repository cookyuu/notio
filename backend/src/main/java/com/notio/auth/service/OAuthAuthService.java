package com.notio.auth.service;

import com.notio.auth.adapter.AuthProviderAdapter;
import com.notio.auth.adapter.AuthProviderAdapterRegistry;
import com.notio.auth.config.AuthProperties;
import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.AuthProviderState;
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
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAuthService {

    private final AuthProviderAdapterRegistry authProviderAdapterRegistry;
    private final AuthProviderStateRepository authProviderStateRepository;
    private final AuthAuditService authAuditService;
    private final AuthProperties authProperties;

    @Transactional
    public OAuthStartResponse start(final OAuthStartRequest request) {
        final AuthProviderAdapter adapter = authProviderAdapterRegistry.get(request.getProvider());
        final OffsetDateTime expiresAt = OffsetDateTime.now().plus(authProperties.getOauth().getStateTtl());
        final AuthProviderState providerState = authProviderStateRepository.save(AuthProviderState.builder()
                .provider(request.getProvider())
                .state(UUID.randomUUID().toString())
                .platform(request.getPlatform())
                .redirectUri(request.getRedirectUri().trim())
                .pkceVerifier(normalizePkceVerifier(request.getPkceVerifier()))
                .expiresAt(expiresAt)
                .build());

        final String authorizationUrl = adapter.buildAuthorizationUrl(providerState);
        authAuditService.recordOAuthStart(request.getProvider(), request.getPlatform());
        log.info("OAuth start initialized: provider={}", request.getProvider());

        return OAuthStartResponse.builder()
                .authorizationUrl(authorizationUrl)
                .state(providerState.getState())
                .build();
    }

    public String callback(
            final String providerValue,
            final String state,
            final String code,
            final String error
    ) {
        final AuthProvider provider = resolveProvider(providerValue);
        final AuthProviderState providerState = validateState(provider, state);

        if (error != null && !error.isBlank()) {
            authAuditService.recordOAuthCallbackFailure(provider, providerState.getPlatform(), "provider_error");
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED, "OAuth provider returned error: " + error);
        }
        if (code == null || code.isBlank()) {
            authAuditService.recordOAuthCallbackFailure(provider, providerState.getPlatform(), "missing_code");
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED, "OAuth authorization code is missing.");
        }

        authAuditService.recordOAuthCallbackSuccess(provider, providerState.getPlatform());
        return UriComponentsBuilder.fromUriString(providerState.getRedirectUri())
                .queryParam("provider", provider.name())
                .queryParam("code", code.trim())
                .queryParam("state", providerState.getState())
                .build(true)
                .toUriString();
    }

    public OAuthExchangeResponse exchange(final OAuthExchangeRequest request) {
        final AuthProviderState providerState = validateState(request.getProvider(), request.getState());
        validateExchangeRequest(providerState, request.getPlatform(), request.getRedirectUri());
        final AuthProviderAdapter adapter = authProviderAdapterRegistry.get(request.getProvider());

        try {
            return adapter.exchangeAuthorizationCode(request.getCode().trim(), providerState);
        } catch (NotioException exception) {
            authAuditService.recordOAuthCallbackFailure(
                    request.getProvider(),
                    providerState.getPlatform(),
                    exception.getErrorCode().getCode()
            );
            throw exception;
        } catch (RuntimeException exception) {
            authAuditService.recordOAuthCallbackFailure(request.getProvider(), providerState.getPlatform(), "exchange_exception");
            throw new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED);
        }
    }

    private AuthProviderState validateState(final AuthProvider provider, final String stateValue) {
        final String normalizedState = stateValue == null ? "" : stateValue.trim();
        final AuthProviderState providerState = authProviderStateRepository.findByState(normalizedState)
                .orElseThrow(() -> {
                    authAuditService.recordOAuthCallbackFailure(provider, null, "state_missing");
                    return new NotioException(ErrorCode.OAUTH_STATE_INVALID);
                });

        if (providerState.getProvider() != provider || providerState.isExpired()) {
            authAuditService.recordOAuthCallbackFailure(provider, providerState.getPlatform(), "state_invalid");
            throw new NotioException(ErrorCode.OAUTH_STATE_INVALID);
        }

        return providerState;
    }

    private void validateExchangeRequest(
            final AuthProviderState providerState,
            final AuthPlatform requestedPlatform,
            final String requestedRedirectUri
    ) {
        final String normalizedRedirectUri = requestedRedirectUri == null ? "" : requestedRedirectUri.trim();
        if (providerState.getPlatform() != requestedPlatform || !providerState.getRedirectUri().equals(normalizedRedirectUri)) {
            authAuditService.recordOAuthCallbackFailure(providerState.getProvider(), providerState.getPlatform(), "state_context_mismatch");
            throw new NotioException(ErrorCode.OAUTH_STATE_INVALID);
        }
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
