package com.notio.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.auth.config.JwtProperties;
import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.User;
import com.notio.auth.domain.UserStatus;
import com.notio.auth.dto.LoginRequest;
import com.notio.auth.dto.LoginResponse;
import com.notio.auth.repository.AuthIdentityRepository;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.exception.NotioException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        when(jwtTokenProvider.getExpirationTime("refresh-token")).thenReturn(java.time.OffsetDateTime.now().plusDays(7));
        when(jwtProperties.getExpiration()).thenReturn(86400000L);

        final LoginResponse response = authService.login(request);

        assertThat(response.getUserId()).isEqualTo("1");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        final ArgumentCaptor<com.notio.auth.domain.RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(com.notio.auth.domain.RefreshToken.class);
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
}
