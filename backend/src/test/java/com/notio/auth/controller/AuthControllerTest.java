package com.notio.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.dto.AuthUserResponse;
import com.notio.auth.dto.FindIdRequest;
import com.notio.auth.dto.FindIdResponse;
import com.notio.auth.dto.OAuthExchangeRequest;
import com.notio.auth.dto.OAuthExchangeResponse;
import com.notio.auth.dto.OAuthStartRequest;
import com.notio.auth.dto.OAuthStartResponse;
import com.notio.auth.dto.PasswordResetConfirmRequest;
import com.notio.auth.dto.PasswordResetConfirmResponse;
import com.notio.auth.dto.PasswordResetRequestRequest;
import com.notio.auth.dto.PasswordResetRequestResponse;
import com.notio.auth.dto.SignupRequest;
import com.notio.auth.dto.SignupResponse;
import com.notio.auth.service.AuthService;
import com.notio.auth.service.LocalAuthService;
import com.notio.auth.service.OAuthAuthService;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthControllerTest {

    @Test
    void signupReturnsWrappedSuccessResponse() {
        final AuthService authService = mock(AuthService.class);
        final LocalAuthService localAuthService = mock(LocalAuthService.class);
        final OAuthAuthService oAuthAuthService = mock(OAuthAuthService.class);
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final AuthController controller = new AuthController(authService, localAuthService, oAuthAuthService, jwtTokenProvider);

        when(localAuthService.signup(org.mockito.ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(SignupResponse.builder()
                        .message("회원가입이 완료되었습니다.")
                        .build());

        final var responseEntity = controller.signup(SignupRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build());
        final ApiResponse<SignupResponse> response = responseEntity.getBody();

        assertThat(response).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.success()).isTrue();
        assertThat(response.data().getMessage()).isEqualTo("회원가입이 완료되었습니다.");
    }

    @Test
    void findIdReturnsWrappedSuccessResponse() {
        final AuthController controller = new AuthController(
                mock(AuthService.class),
                localAuthServiceWithFindId(),
                mock(OAuthAuthService.class),
                mock(JwtTokenProvider.class)
        );

        final ApiResponse<FindIdResponse> response = controller.findId(FindIdRequest.builder()
                .email("user@example.com")
                .build()).getBody();

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.data().getMessage()).isNotBlank();
    }

    @Test
    void passwordResetEndpointsReturnWrappedSuccessResponse() {
        final AuthService authService = mock(AuthService.class);
        final LocalAuthService localAuthService = mock(LocalAuthService.class);
        final OAuthAuthService oAuthAuthService = mock(OAuthAuthService.class);
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final AuthController controller = new AuthController(authService, localAuthService, oAuthAuthService, jwtTokenProvider);

        when(localAuthService.requestPasswordReset(org.mockito.ArgumentMatchers.any(PasswordResetRequestRequest.class)))
                .thenReturn(PasswordResetRequestResponse.builder().message("requested").build());
        when(localAuthService.confirmPasswordReset(org.mockito.ArgumentMatchers.any(PasswordResetConfirmRequest.class)))
                .thenReturn(PasswordResetConfirmResponse.builder().message("confirmed").build());

        final ApiResponse<PasswordResetRequestResponse> requestResponse = controller.requestPasswordReset(
                PasswordResetRequestRequest.builder().email("user@example.com").build()).getBody();
        final ApiResponse<PasswordResetConfirmResponse> confirmResponse = controller.confirmPasswordReset(
                PasswordResetConfirmRequest.builder().token("token").newPassword("newPassword123").build()).getBody();

        assertThat(requestResponse).isNotNull();
        assertThat(confirmResponse).isNotNull();
        assertThat(requestResponse.success()).isTrue();
        assertThat(confirmResponse.success()).isTrue();
    }

    @Test
    void oauthEndpointsReturnWrappedSuccessResponse() {
        final AuthService authService = mock(AuthService.class);
        final LocalAuthService localAuthService = mock(LocalAuthService.class);
        final OAuthAuthService oAuthAuthService = mock(OAuthAuthService.class);
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final AuthController controller = new AuthController(authService, localAuthService, oAuthAuthService, jwtTokenProvider);

        when(oAuthAuthService.start(org.mockito.ArgumentMatchers.any(OAuthStartRequest.class)))
                .thenReturn(OAuthStartResponse.builder()
                        .authorizationUrl("https://example.com/oauth")
                        .state("state-1")
                        .build());
        when(oAuthAuthService.callback("google", "state-1", "code-1", null))
                .thenReturn("https://app.notio.dev/callback?provider=GOOGLE&code=code-1&state=state-1");
        when(oAuthAuthService.exchange(org.mockito.ArgumentMatchers.any(OAuthExchangeRequest.class)))
                .thenReturn(OAuthExchangeResponse.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .tokenType("Bearer")
                        .expiresIn(3600)
                        .user(AuthUserResponse.builder()
                                .id(1L)
                                .primaryEmail("user@example.com")
                                .displayName("Notio User")
                                .build())
                        .build());

        final ApiResponse<OAuthStartResponse> startResponse = controller.startOAuth(OAuthStartRequest.builder()
                .provider(AuthProvider.GOOGLE)
                .platform(AuthPlatform.WEB)
                .redirectUri("https://app.notio.dev/callback")
                .build()).getBody();
        final var callbackResponse = controller.oauthCallback(
                "google", "state-1", "code-1", null);
        final ApiResponse<OAuthExchangeResponse> exchangeResponse = controller.exchangeOAuth(OAuthExchangeRequest.builder()
                .provider(AuthProvider.GOOGLE)
                .platform(AuthPlatform.WEB)
                .state("state-1")
                .code("code-1")
                .redirectUri("https://app.notio.dev/callback")
                .build()).getBody();

        assertThat(startResponse).isNotNull();
        assertThat(callbackResponse).isNotNull();
        assertThat(exchangeResponse).isNotNull();
        assertThat(startResponse.success()).isTrue();
        assertThat(startResponse.data().getAuthorizationUrl()).isEqualTo("https://example.com/oauth");
        assertThat(callbackResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(callbackResponse.getHeaders().getLocation()).hasToString(
                "https://app.notio.dev/callback?provider=GOOGLE&code=code-1&state=state-1"
        );
        assertThat(exchangeResponse.success()).isTrue();
    }

    private LocalAuthService localAuthServiceWithFindId() {
        final LocalAuthService localAuthService = mock(LocalAuthService.class);
        when(localAuthService.findId(org.mockito.ArgumentMatchers.any(FindIdRequest.class)))
                .thenReturn(FindIdResponse.builder().message("ok").build());
        return localAuthService;
    }
}
