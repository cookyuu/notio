package com.notio.auth.controller;

import com.notio.auth.dto.FindIdRequest;
import com.notio.auth.dto.FindIdResponse;
import com.notio.auth.dto.LoginRequest;
import com.notio.auth.dto.LoginResponse;
import com.notio.auth.dto.LogoutResponse;
import com.notio.auth.dto.OAuthCallbackResponse;
import com.notio.auth.dto.OAuthExchangeRequest;
import com.notio.auth.dto.OAuthExchangeResponse;
import com.notio.auth.dto.OAuthStartRequest;
import com.notio.auth.dto.OAuthStartResponse;
import com.notio.auth.dto.PasswordResetConfirmRequest;
import com.notio.auth.dto.PasswordResetConfirmResponse;
import com.notio.auth.dto.PasswordResetRequestRequest;
import com.notio.auth.dto.PasswordResetRequestResponse;
import com.notio.auth.dto.RefreshRequest;
import com.notio.auth.dto.RefreshResponse;
import com.notio.auth.dto.SignupRequest;
import com.notio.auth.dto.SignupResponse;
import com.notio.auth.service.AuthService;
import com.notio.auth.service.LocalAuthService;
import com.notio.auth.service.OAuthAuthService;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;
    private final LocalAuthService localAuthService;
    private final OAuthAuthService oAuthAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "로컬 이메일 계정을 생성합니다.")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody final SignupRequest request) {
        final SignupResponse response = localAuthService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/find-id")
    @Operation(summary = "아이디 찾기", description = "계정 존재 여부를 노출하지 않고 계정 안내 요청을 접수합니다.")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@Valid @RequestBody final FindIdRequest request) {
        final FindIdResponse response = localAuthService.findId(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "비밀번호 재설정 요청", description = "비밀번호 재설정 요청을 접수하고 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<PasswordResetRequestResponse>> requestPasswordReset(
            @Valid @RequestBody final PasswordResetRequestRequest request) {
        final PasswordResetRequestResponse response = localAuthService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "비밀번호 재설정 확정", description = "재설정 토큰을 검증하고 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<PasswordResetConfirmResponse>> confirmPasswordReset(
            @Valid @RequestBody final PasswordResetConfirmRequest request) {
        final PasswordResetConfirmResponse response = localAuthService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/oauth/start")
    @Operation(summary = "OAuth 인증 시작", description = "OAuth provider 인증 시작에 필요한 state와 인가 URL을 발급합니다.")
    public ResponseEntity<ApiResponse<OAuthStartResponse>> startOAuth(@Valid @RequestBody final OAuthStartRequest request) {
        final OAuthStartResponse response = oAuthAuthService.start(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/oauth/callback/{provider}")
    @Operation(summary = "OAuth 콜백", description = "OAuth 콜백 요청의 provider/state를 검증하고 후속 교환 흐름으로 전달합니다.")
    public ResponseEntity<ApiResponse<OAuthCallbackResponse>> oauthCallback(
            @PathVariable final String provider,
            @RequestParam("state") final String state,
            @RequestParam(value = "code", required = false) final String code,
            @RequestParam(value = "error", required = false) final String error) {
        final OAuthCallbackResponse response = oAuthAuthService.callback(provider, state, code, error);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/oauth/exchange")
    @Operation(summary = "OAuth 코드 교환", description = "OAuth provider authorization code를 서비스 토큰 또는 앱 세션으로 교환합니다.")
    public ResponseEntity<ApiResponse<OAuthExchangeResponse>> exchangeOAuth(
            @Valid @RequestBody final OAuthExchangeRequest request) {
        final OAuthExchangeResponse response = oAuthAuthService.exchange(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody final LoginRequest request) {
        final LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh token으로 새로운 Access token과 Refresh token을 발급받습니다.")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody final RefreshRequest request) {
        final RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자의 세션을 종료하고 Refresh token을 무효화합니다.")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @RequestHeader("Authorization") final String authorizationHeader) {
        // Bearer 토큰 추출
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }

        final String accessToken = authorizationHeader.substring(7);

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new NotioException(ErrorCode.INVALID_TOKEN);
        }

        // 사용자 ID 추출
        final String userId = jwtTokenProvider.getUserId(accessToken);

        // 로그아웃 처리
        authService.logout(userId);

        return ResponseEntity.ok(ApiResponse.success(LogoutResponse.success()));
    }
}
