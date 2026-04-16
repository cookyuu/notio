package com.notio.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.notio.auth.dto.FindIdRequest;
import com.notio.auth.dto.FindIdResponse;
import com.notio.auth.dto.PasswordResetConfirmRequest;
import com.notio.auth.dto.PasswordResetConfirmResponse;
import com.notio.auth.dto.PasswordResetRequestRequest;
import com.notio.auth.dto.PasswordResetRequestResponse;
import com.notio.auth.dto.SignupRequest;
import com.notio.auth.dto.SignupResponse;
import com.notio.auth.service.AuthService;
import com.notio.auth.service.LocalAuthService;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

class AuthControllerTest {

    @Test
    void signupReturnsWrappedSuccessResponse() {
        final AuthService authService = mock(AuthService.class);
        final LocalAuthService localAuthService = mock(LocalAuthService.class);
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final AuthController controller = new AuthController(authService, localAuthService, jwtTokenProvider);

        when(localAuthService.signup(org.mockito.ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(SignupResponse.builder()
                        .userId("1")
                        .email("user@example.com")
                        .displayName("Notio")
                        .build());

        final ApiResponse<SignupResponse> response = controller.signup(SignupRequest.builder()
                .email("user@example.com")
                .password("password123")
                .displayName("Notio")
                .build()).getBody();

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.data().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void findIdReturnsWrappedSuccessResponse() {
        final AuthController controller = new AuthController(
                mock(AuthService.class),
                localAuthServiceWithFindId(),
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
        final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        final AuthController controller = new AuthController(authService, localAuthService, jwtTokenProvider);

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

    private LocalAuthService localAuthServiceWithFindId() {
        final LocalAuthService localAuthService = mock(LocalAuthService.class);
        when(localAuthService.findId(org.mockito.ArgumentMatchers.any(FindIdRequest.class)))
                .thenReturn(FindIdResponse.builder().message("ok").build());
        return localAuthService;
    }
}
