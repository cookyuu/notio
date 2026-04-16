package com.notio.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.PasswordResetToken;
import com.notio.auth.domain.User;
import com.notio.auth.domain.UserStatus;
import com.notio.auth.dto.FindIdRequest;
import com.notio.auth.dto.PasswordResetConfirmRequest;
import com.notio.auth.dto.PasswordResetRequestRequest;
import com.notio.auth.dto.SignupRequest;
import com.notio.auth.repository.AuthIdentityRepository;
import com.notio.auth.repository.PasswordResetTokenRepository;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.util.AuthTokenUtils;
import com.notio.common.exception.NotioException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks
    private LocalAuthService localAuthService;

    @Test
    void signupCreatesUserAndLocalIdentity() {
        final SignupRequest request = SignupRequest.builder()
                .email("USER@example.com")
                .password("password123")
                .displayName(" Notio ")
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

        assertThat(response.getUserId()).isEqualTo("1");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getDisplayName()).isEqualTo("Notio");

        final ArgumentCaptor<AuthIdentity> authIdentityCaptor = ArgumentCaptor.forClass(AuthIdentity.class);
        verify(authIdentityRepository).save(authIdentityCaptor.capture());
        assertThat(authIdentityCaptor.getValue().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(authIdentityCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(authIdentityCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void signupRejectsDuplicateLocalEmail() {
        when(authIdentityRepository.existsActiveLocalByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> localAuthService.signup(SignupRequest.builder()
                .email("user@example.com")
                .password("password123")
                .displayName("Notio")
                .build())).isInstanceOf(NotioException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findIdReturnsSameResponseWhetherAccountExistsOrNot() {
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

        final var existingResponse = localAuthService.findId(request);
        final var missingResponse = localAuthService.findId(request);

        assertThat(existingResponse.getMessage()).isEqualTo(missingResponse.getMessage());
    }

    @Test
    void requestPasswordResetReturnsSameResponseWhetherAccountExistsOrNot() {
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

        final var existingResponse = localAuthService.requestPasswordReset(request);
        final var missingResponse = localAuthService.requestPasswordReset(request);

        assertThat(existingResponse.getMessage()).isEqualTo(missingResponse.getMessage());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void confirmPasswordResetRejectsExpiredToken() {
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
    void confirmPasswordResetUpdatesPasswordMarksTokenUsedAndRevokesRefreshTokens() {
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
