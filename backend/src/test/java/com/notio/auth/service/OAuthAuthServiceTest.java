package com.notio.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.notio.auth.adapter.AuthProviderAdapterRegistry;
import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.AuthProviderState;
import com.notio.auth.dto.OAuthExchangeRequest;
import com.notio.auth.dto.OAuthStartRequest;
import com.notio.auth.repository.AuthProviderStateRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthAuthServiceTest {

    @Mock
    private AuthProviderAdapterRegistry authProviderAdapterRegistry;

    @Mock
    private AuthProviderStateRepository authProviderStateRepository;

    @InjectMocks
    private OAuthAuthService oAuthAuthService;

    @Test
    void startRejectsUnsupportedProviderWhenAdapterIsMissing() {
        when(authProviderAdapterRegistry.get(AuthProvider.GOOGLE))
                .thenThrow(new NotioException(ErrorCode.AUTH_PROVIDER_UNSUPPORTED));

        assertThatThrownBy(() -> oAuthAuthService.start(OAuthStartRequest.builder()
                .provider(AuthProvider.GOOGLE)
                .platform(AuthPlatform.WEB)
                .redirectUri("https://app.notio.dev/callback")
                .build())).isInstanceOf(NotioException.class);
    }

    @Test
    void callbackRejectsInvalidStateBeforeAdapterResolution() {
        when(authProviderStateRepository.findByState("missing-state")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthAuthService.callback("google", "missing-state", "code", null))
                .isInstanceOf(NotioException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_STATE_INVALID);
    }

    @Test
    void exchangeRejectsExpiredState() {
        when(authProviderStateRepository.findByState("expired-state"))
                .thenReturn(Optional.of(AuthProviderState.builder()
                        .provider(AuthProvider.GOOGLE)
                        .state("expired-state")
                        .platform(AuthPlatform.WEB)
                        .redirectUri("https://app.notio.dev/callback")
                        .expiresAt(OffsetDateTime.now().minusMinutes(1))
                        .build()));

        assertThatThrownBy(() -> oAuthAuthService.exchange(OAuthExchangeRequest.builder()
                .provider(AuthProvider.GOOGLE)
                .state("expired-state")
                .code("code")
                .build())).isInstanceOf(NotioException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_STATE_INVALID);
    }
}
