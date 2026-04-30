package com.notio.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notio.auth.config.AuthProperties;
import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.PasswordResetToken;
import com.notio.auth.domain.User;
import com.notio.auth.domain.UserStatus;
import com.notio.auth.dto.FindIdRequest;
import com.notio.auth.dto.PasswordResetConfirmRequest;
import com.notio.auth.dto.PasswordResetRequestRequest;
import com.notio.auth.dto.SignupRequest;
import com.notio.auth.mail.AuthMailMessage;
import com.notio.auth.mail.AuthMailSender;
import com.notio.auth.mail.AuthMailTemplateService;
import com.notio.auth.repository.AuthIdentityRepository;
import com.notio.auth.repository.PasswordResetTokenRepository;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.util.AuthTokenUtils;
import com.notio.common.exception.NotioException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthMailTemplateService authMailTemplateService;

    @Mock
    private AuthMailSender authMailSender;

    @Mock
    private AuthAuditService authAuditService;

    private LocalAuthService localAuthService;

    private AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        localAuthService = new LocalAuthService(
                userRepository,
                authIdentityRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                authMailTemplateService,
                authMailSender,
                authAuditService,
                authProperties
        );
    }

    @Test
    void signupCreatesUserAndLocalIdentity() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
        final SignupRequest request = SignupRequest.builder()
                .displayName("  Notio User  ")
                .email("USER@example.com")
                .password("password123")
                .build();
        when(authIdentityRepository.existsActiveLocalByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            final User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .primaryEmail(user.getPrimaryEmail())
                    .displayName(user.getDisplayName())
                    .status(user.getStatus())
                    .build();
        });

        final var response = localAuthService.signup(request);

        assertThat(response.getMessage()).isEqualTo("회원가입이 완료되었습니다.");

        final ArgumentCaptor<AuthIdentity> authIdentityCaptor = ArgumentCaptor.forClass(AuthIdentity.class);
        verify(authIdentityRepository).save(authIdentityCaptor.capture());
        assertThat(authIdentityCaptor.getValue().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(authIdentityCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(authIdentityCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(authIdentityCaptor.getValue().getUser().getDisplayName()).isEqualTo("Notio User");
    }

    @Test
    void signupLogsMaskedEmailOnly() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
        final SignupRequest request = SignupRequest.builder()
                .displayName("Notio User")
                .email("USER@example.com")
                .password("password123")
                .build();
        when(authIdentityRepository.existsActiveLocalByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(User.builder()
                .id(1L)
                .primaryEmail("user@example.com")
                .displayName("Notio User")
                .status(UserStatus.ACTIVE)
                .build());

        final Logger logger = (Logger) LoggerFactory.getLogger(LocalAuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        try {
            localAuthService.signup(request);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).contains("event=auth_signup_succeeded");
        assertThat(event.getFormattedMessage()).contains("masked_email=u***@example.com");
        assertThat(event.getFormattedMessage()).doesNotContain("USER@example.com");
        assertThat(event.getFormattedMessage()).doesNotContain("password123");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "auth_signup_succeeded");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "success");
    }

    @Test
    void signupRejectsDuplicateLocalEmail() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
        when(authIdentityRepository.existsActiveLocalByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> localAuthService.signup(SignupRequest.builder()
                .displayName("Notio User")
                .email("user@example.com")
                .password("password123")
                .build())).isInstanceOf(NotioException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findIdReturnsSameResponseWhetherAccountExistsOrNot() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
        final FindIdRequest request = FindIdRequest.builder()
                .email("user@example.com")
                .build();
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
                .passwordHash("encoded")
                .build();

        when(authIdentityRepository.findActiveLocalByEmail("user@example.com"))
                .thenReturn(Optional.of(authIdentity), Optional.empty());
        when(authMailTemplateService.buildFindIdMessage(user))
                .thenReturn(new AuthMailMessage("user@example.com", "subject", "body", null));

        final var existingResponse = localAuthService.findId(request);
        final var missingResponse = localAuthService.findId(request);

        assertThat(existingResponse.getMessage()).isEqualTo(missingResponse.getMessage());
        verify(authMailSender).send(any(AuthMailMessage.class));
    }

    @Test
    void requestPasswordResetReturnsSameResponseWhetherAccountExistsOrNot() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(15));
        final PasswordResetRequestRequest request = PasswordResetRequestRequest.builder()
                .email("user@example.com")
                .build();
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
                .passwordHash("encoded")
                .build();

        when(authIdentityRepository.findActiveLocalByEmail("user@example.com"))
                .thenReturn(Optional.of(authIdentity), Optional.empty());
        when(authMailTemplateService.buildPasswordResetMessage(eq(user), anyString()))
                .thenReturn(new AuthMailMessage("user@example.com", "subject", "body", null));

        final var existingResponse = localAuthService.requestPasswordReset(request);
        final var missingResponse = localAuthService.requestPasswordReset(request);

        assertThat(existingResponse.getMessage()).isEqualTo(missingResponse.getMessage());
        final ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now().plusMinutes(14));
        verify(authMailSender).send(any(AuthMailMessage.class));
    }

    @Test
    void requestPasswordResetLogsMaskedEmailWithoutTokenLeak() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(15));
        final PasswordResetRequestRequest request = PasswordResetRequestRequest.builder()
                .email("user@example.com")
                .build();
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
                .passwordHash("encoded")
                .build();

        when(authIdentityRepository.findActiveLocalByEmail("user@example.com"))
                .thenReturn(Optional.of(authIdentity));
        when(authMailTemplateService.buildPasswordResetMessage(eq(user), anyString()))
                .thenReturn(new AuthMailMessage("user@example.com", "subject", "body", null));

        final Logger logger = (Logger) LoggerFactory.getLogger(LocalAuthService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        try {
            localAuthService.requestPasswordReset(request);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).contains("event=auth_password_reset_requested");
        assertThat(event.getFormattedMessage()).contains("user_id=1");
        assertThat(event.getFormattedMessage()).contains("masked_email=u***@example.com");
        assertThat(event.getFormattedMessage()).doesNotContain("subject");
        assertThat(event.getFormattedMessage()).doesNotContain("body");
        assertThat(event.getMDCPropertyMap()).containsEntry("event", "auth_password_reset_requested");
        assertThat(event.getMDCPropertyMap()).containsEntry("outcome", "accepted");
    }

    @Test
    void confirmPasswordResetRejectsExpiredToken() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
        final String rawToken = "expired-token";
        when(passwordResetTokenRepository.findByTokenHash(AuthTokenUtils.sha256Hex(rawToken)))
                .thenReturn(Optional.of(PasswordResetToken.builder()
                        .tokenHash(AuthTokenUtils.sha256Hex(rawToken))
                        .expiresAt(OffsetDateTime.now().minusMinutes(1))
                        .user(User.builder().id(1L).primaryEmail("user@example.com").displayName("user").status(UserStatus.ACTIVE).build())
                        .authIdentity(AuthIdentity.builder()
                                .id(10L)
                                .provider(AuthProvider.LOCAL)
                                .email("user@example.com")
                                .passwordHash("encoded")
                                .user(User.builder().id(1L).primaryEmail("user@example.com").displayName("user").status(UserStatus.ACTIVE).build())
                                .build())
                        .build()));

        assertThatThrownBy(() -> localAuthService.confirmPasswordReset(PasswordResetConfirmRequest.builder()
                .token(rawToken)
                .newPassword("newPassword123")
                .build())).isInstanceOf(NotioException.class);
    }

    @Test
    void confirmPasswordResetRejectsForgedToken() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
        final String rawToken = "forged-token";
        when(passwordResetTokenRepository.findByTokenHash(AuthTokenUtils.sha256Hex(rawToken)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> localAuthService.confirmPasswordReset(PasswordResetConfirmRequest.builder()
                .token(rawToken)
                .newPassword("newPassword123")
                .build())).isInstanceOf(NotioException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenRepository, never()).revokeAllByUser(any(User.class));
    }

    @Test
    void confirmPasswordResetRejectsReusedToken() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
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
                .passwordHash("encoded")
                .build();
        final String rawToken = "used-token";
        when(passwordResetTokenRepository.findByTokenHash(AuthTokenUtils.sha256Hex(rawToken)))
                .thenReturn(Optional.of(PasswordResetToken.builder()
                        .user(user)
                        .authIdentity(authIdentity)
                        .tokenHash(AuthTokenUtils.sha256Hex(rawToken))
                        .expiresAt(OffsetDateTime.now().plusMinutes(5))
                        .usedAt(OffsetDateTime.now().minusMinutes(1))
                        .build()));

        assertThatThrownBy(() -> localAuthService.confirmPasswordReset(PasswordResetConfirmRequest.builder()
                .token(rawToken)
                .newPassword("newPassword123")
                .build())).isInstanceOf(NotioException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenRepository, never()).revokeAllByUser(any(User.class));
    }

    @Test
    void confirmPasswordResetUpdatesPasswordMarksTokenUsedAndRevokesRefreshTokens() {
        authProperties.getPasswordReset().setTokenTtl(Duration.ofMinutes(30));
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
                .passwordHash("encoded")
                .build();
        final String rawToken = "valid-token";
        final PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .authIdentity(authIdentity)
                .tokenHash(AuthTokenUtils.sha256Hex(rawToken))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(AuthTokenUtils.sha256Hex(rawToken)))
                .thenReturn(Optional.of(passwordResetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded");

        final var response = localAuthService.confirmPasswordReset(PasswordResetConfirmRequest.builder()
                .token(rawToken)
                .newPassword("newPassword123")
                .build());

        assertThat(response.getMessage()).isEqualTo("비밀번호가 재설정되었습니다.");
        assertThat(authIdentity.getPasswordHash()).isEqualTo("new-encoded");
        assertThat(passwordResetToken.getUsedAt()).isNotNull();
        verify(refreshTokenRepository).revokeAllByUser(user);
    }
}
