package com.notio.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notio.auth.adapter.AuthProviderAdapter;
import com.notio.auth.adapter.AuthProviderAdapterRegistry;
import com.notio.auth.config.AuthProperties;
import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.AuthProviderState;
import com.notio.auth.dto.OAuthExchangeRequest;
import com.notio.auth.dto.OAuthStartRequest;
import com.notio.auth.repository.AuthProviderStateRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class OAuthAuthServiceTest {

    @Mock
    private AuthProviderAdapterRegistry authProviderAdapterRegistry;

    @Mock
    private AuthProviderStateRepository authProviderStateRepository;

    @Mock
    private AuthAuditService authAuditService;

    @Mock
    private AuthProviderAdapter authProviderAdapter;

    private OAuthAuthService oAuthAuthService;

    private AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        oAuthAuthService = new OAuthAuthService(
                authProviderAdapterRegistry,
                authProviderStateRepository,
                authAuditService,
                authProperties
        );
    }

    @Test
    void startRejectsUnsupportedProviderWhenAdapterIsMissing() {
        authProperties.getOauth().setStateTtl(Duration.ofMinutes(5));
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
        authProperties.getOauth().setStateTtl(Duration.ofMinutes(5));
        when(authProviderStateRepository.findByState("missing-state")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthAuthService.callback("google", "missing-state", "code", null))
                .isInstanceOf(NotioException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_STATE_INVALID);
    }

    @Test
    void exchangeRejectsExpiredState() {
        authProperties.getOauth().setStateTtl(Duration.ofMinutes(5));
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
                .platform(AuthPlatform.WEB)
                .state("expired-state")
                .code("code")
                .redirectUri("https://app.notio.dev/callback")
                .build())).isInstanceOf(NotioException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_STATE_INVALID);
    }

    @Test
    void callbackLogsValidatedEventWithoutCodeOrStateLeak() {
        authProperties.getOauth().setStateTtl(Duration.ofMinutes(5));
        when(authProviderStateRepository.findByState("safe-state"))
                .thenReturn(Optional.of(AuthProviderState.builder()
                        .provider(AuthProvider.GOOGLE)
                        .state("safe-state")
                        .platform(AuthPlatform.WEB)
                        .redirectUri("https://app.notio.dev/callback")
                        .expiresAt(OffsetDateTime.now().plusMinutes(1))
                        .build()));

        final Logger logger = (Logger) LoggerFactory.getLogger(OAuthAuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        final String redirectUri;
        try {
            redirectUri = oAuthAuthService.callback("google", "safe-state", "secret-code", null);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(redirectUri).contains("code=secret-code");
        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).contains("event=oauth_callback_validated");
        assertThat(event.getFormattedMessage()).contains("provider=GOOGLE");
        assertThat(event.getFormattedMessage()).contains("platform=WEB");
        assertThat(event.getFormattedMessage()).doesNotContain("secret-code");
        assertThat(event.getFormattedMessage()).doesNotContain("safe-state");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "oauth_callback_validated");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "success");
    }

    @Test
    void exchangeLogsFailureWithoutAuthorizationCodeLeak() {
        authProperties.getOauth().setStateTtl(Duration.ofMinutes(5));
        when(authProviderStateRepository.findByState("valid-state"))
                .thenReturn(Optional.of(AuthProviderState.builder()
                        .provider(AuthProvider.GOOGLE)
                        .state("valid-state")
                        .platform(AuthPlatform.WEB)
                        .redirectUri("https://app.notio.dev/callback")
                        .expiresAt(OffsetDateTime.now().plusMinutes(1))
                        .build()));
        when(authProviderAdapterRegistry.get(AuthProvider.GOOGLE)).thenReturn(authProviderAdapter);
        when(authProviderAdapter.exchangeAuthorizationCode(any(), any()))
                .thenThrow(new NotioException(ErrorCode.OAUTH_CALLBACK_FAILED));

        final Logger logger = (Logger) LoggerFactory.getLogger(OAuthAuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> oAuthAuthService.exchange(OAuthExchangeRequest.builder()
                    .provider(AuthProvider.GOOGLE)
                    .platform(AuthPlatform.WEB)
                    .state("valid-state")
                    .code("secret-code")
                    .redirectUri("https://app.notio.dev/callback")
                    .build())).isInstanceOf(NotioException.class);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).contains("event=oauth_exchange_failed");
        assertThat(event.getFormattedMessage()).contains("reason_category=OAUTH_CALLBACK_FAILED");
        assertThat(event.getFormattedMessage()).doesNotContain("secret-code");
        assertThat(event.getFormattedMessage()).doesNotContain("valid-state");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "oauth_exchange_failed");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
    }
}
