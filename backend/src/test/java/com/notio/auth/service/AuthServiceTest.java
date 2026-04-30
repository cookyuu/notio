package com.notio.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notio.auth.config.JwtProperties;
import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.RefreshToken;
import com.notio.auth.domain.User;
import com.notio.auth.domain.UserStatus;
import com.notio.auth.dto.LoginRequest;
import com.notio.auth.dto.LoginResponse;
import com.notio.auth.dto.RefreshRequest;
import com.notio.auth.dto.RefreshResponse;
import com.notio.auth.repository.AuthIdentityRepository;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.exception.NotioException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginUsesLocalAuthIdentity() {
        final User user = User.builder()
                .id(1L)
                .primaryEmail("user@example.com")
                .displayName("user")
                .status(UserStatus.ACTIVE)
                .build();
        final AuthIdentity authIdentity = AuthIdentity.builder()
                .id(10L)
                .user(user)
                .provider(AuthProvider.LOCAL)
                .email("user@example.com")
                .passwordHash("encoded-password")
                .emailVerified(true)
                .build();
        final LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        when(authIdentityRepository.findActiveByProviderAndEmail(AuthProvider.LOCAL, "user@example.com"))
                .thenReturn(Optional.of(authIdentity));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("1", "user@example.com")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("1")).thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationTime("refresh-token")).thenReturn(OffsetDateTime.now().plusDays(7));
        when(jwtProperties.getExpiration()).thenReturn(86400000L);

        final LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getPrimaryEmail()).isEqualTo("user@example.com");

        final ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatchStoredIdentity() {
        final User user = User.builder()
                .id(1L)
                .primaryEmail("user@example.com")
                .displayName("user")
                .status(UserStatus.ACTIVE)
                .build();
        final AuthIdentity authIdentity = AuthIdentity.builder()
                .id(10L)
                .user(user)
                .provider(AuthProvider.LOCAL)
                .email("user@example.com")
                .passwordHash("encoded-password")
                .emailVerified(true)
                .build();
        final LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("wrong-password")
                .build();

        when(authIdentityRepository.findActiveByProviderAndEmail(AuthProvider.LOCAL, "user@example.com"))
                .thenReturn(Optional.of(authIdentity));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(NotioException.class);
    }

    @Test
    void loginLogsStandardizedFailureWithoutPasswordLeak() {
        final User user = User.builder()
                .id(1L)
                .primaryEmail("user@example.com")
                .displayName("user")
                .status(UserStatus.ACTIVE)
                .build();
        final AuthIdentity authIdentity = AuthIdentity.builder()
                .id(10L)
                .user(user)
                .provider(AuthProvider.LOCAL)
                .email("user@example.com")
                .passwordHash("encoded-password")
                .emailVerified(true)
                .build();
        final LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("wrong-password")
                .build();

        when(authIdentityRepository.findActiveByProviderAndEmail(AuthProvider.LOCAL, "user@example.com"))
                .thenReturn(Optional.of(authIdentity));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        final Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(NotioException.class);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=auth_login_failed");
        assertThat(event.getFormattedMessage()).contains("masked_email=u***@example.com");
        assertThat(event.getFormattedMessage()).contains("reason_category=password_mismatch");
        assertThat(event.getFormattedMessage()).doesNotContain("wrong-password");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "auth_login_failed");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
    }

    @Test
    void refreshLogsSuccessWithoutTokenLeak() {
        final User user = User.builder()
                .id(1L)
                .primaryEmail("user@example.com")
                .displayName("user")
                .status(UserStatus.ACTIVE)
                .build();
        final RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("incoming-refresh-token")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        final RefreshRequest request = RefreshRequest.builder()
                .refreshToken("incoming-refresh-token")
                .build();

        when(jwtTokenProvider.validateToken("incoming-refresh-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenAndRevokedAtIsNull("incoming-refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateAccessToken("1", "user@example.com")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken("1")).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getExpirationTime("new-refresh-token")).thenReturn(OffsetDateTime.now().plusDays(7));
        when(jwtProperties.getExpiration()).thenReturn(86400000L);

        final Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        final RefreshResponse response;
        try {
            response = authService.refresh(request);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).contains("event=auth_refresh_succeeded");
        assertThat(event.getFormattedMessage()).contains("user_id=1");
        assertThat(event.getFormattedMessage()).contains("masked_email=u***@example.com");
        assertThat(event.getFormattedMessage()).doesNotContain("incoming-refresh-token");
        assertThat(event.getFormattedMessage()).doesNotContain("new-refresh-token");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "auth_refresh_succeeded");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "success");
    }

    @Test
    void refreshLogsFailureWithoutRefreshTokenLeak() {
        final RefreshRequest request = RefreshRequest.builder()
                .refreshToken("incoming-refresh-token")
                .build();

        when(jwtTokenProvider.validateToken("incoming-refresh-token")).thenReturn(false);

        final Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(NotioException.class);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("event=auth_refresh_failed");
        assertThat(event.getFormattedMessage()).contains("reason_category=token_invalid");
        assertThat(event.getFormattedMessage()).doesNotContain("incoming-refresh-token");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "auth_refresh_failed");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "failure");
    }
}
