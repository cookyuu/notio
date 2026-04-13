package com.notio.auth.controller;

import com.notio.auth.dto.LoginRequest;
import com.notio.auth.dto.LoginResponse;
import com.notio.auth.dto.LogoutResponse;
import com.notio.auth.dto.RefreshRequest;
import com.notio.auth.dto.RefreshResponse;
import com.notio.auth.service.AuthService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

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
