package com.notio.auth.service;

import com.notio.auth.config.JwtProperties;
import com.notio.auth.domain.RefreshToken;
import com.notio.auth.domain.User;
import com.notio.auth.dto.LoginRequest;
import com.notio.auth.dto.LoginResponse;
import com.notio.auth.dto.RefreshRequest;
import com.notio.auth.dto.RefreshResponse;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.util.JwtTokenProvider;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 로그인
     */
    @Transactional
    public LoginResponse login(final LoginRequest request) {
        // 사용자 조회
        final User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new NotioException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new NotioException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 토큰 생성
        final String userId = String.valueOf(user.getId());
        final String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getEmail());
        final String refreshTokenValue = jwtTokenProvider.generateRefreshToken(userId);

        // Refresh Token 저장
        final RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(jwtTokenProvider.getExpirationTime(refreshTokenValue))
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully: userId={}, email={}", userId, user.getEmail());

        return LoginResponse.builder()
                .userId(userId)
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn((int) (jwtProperties.getExpiration() / 1000))
                .build();
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public RefreshResponse refresh(final RefreshRequest request) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new NotioException(ErrorCode.INVALID_TOKEN);
        }

        // DB에서 Refresh Token 조회
        final RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedAtIsNull(request.getRefreshToken())
                .orElseThrow(() -> new NotioException(ErrorCode.INVALID_TOKEN));

        // Refresh Token 유효성 검증 (만료 여부)
        if (!refreshToken.isValid()) {
            throw new NotioException(ErrorCode.EXPIRED_TOKEN);
        }

        final User user = refreshToken.getUser();
        final String userId = String.valueOf(user.getId());

        // 새로운 토큰 생성
        final String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getEmail());
        final String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(userId);

        // 기존 Refresh Token 무효화
        refreshToken.revoke();

        // 새로운 Refresh Token 저장
        final RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenValue)
                .expiresAt(jwtTokenProvider.getExpirationTime(newRefreshTokenValue))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refreshed successfully: userId={}", userId);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .expiresIn((int) (jwtProperties.getExpiration() / 1000))
                .build();
    }

    /**
     * 로그아웃 (사용자의 모든 Refresh Token 무효화)
     */
    @Transactional
    public void logout(final String userId) {
        final User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new NotioException(ErrorCode.UNAUTHORIZED));

        // 사용자의 모든 Refresh Token 무효화
        refreshTokenRepository.revokeAllByUser(user);

        log.info("User logged out successfully: userId={}", userId);
    }
}
