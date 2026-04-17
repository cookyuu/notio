package com.notio.auth.service;

import com.notio.auth.config.JwtProperties;
import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.RefreshToken;
import com.notio.auth.domain.User;
import com.notio.auth.dto.LoginRequest;
import com.notio.auth.dto.LoginResponse;
import com.notio.auth.dto.RefreshRequest;
import com.notio.auth.dto.RefreshResponse;
import com.notio.auth.repository.AuthIdentityRepository;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.support.AuthMaskingUtils;
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

    private final AuthIdentityRepository authIdentityRepository;
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
        final AuthIdentity authIdentity = authIdentityRepository
                .findActiveByProviderAndEmail(AuthProvider.LOCAL, request.getEmail())
                .orElseThrow(() -> new NotioException(ErrorCode.INVALID_CREDENTIALS));
        final User user = authIdentity.getUser();

        if (authIdentity.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), authIdentity.getPasswordHash())) {
            throw new NotioException(ErrorCode.INVALID_CREDENTIALS);
        }

        final String userId = String.valueOf(user.getId());
        final String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getPrimaryEmail());
        final String refreshTokenValue = jwtTokenProvider.generateRefreshToken(userId);

        final RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(jwtTokenProvider.getExpirationTime(refreshTokenValue))
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully: userId={}, email={}", userId, AuthMaskingUtils.maskEmail(user.getPrimaryEmail()));

        return LoginResponse.builder()
                .userId(userId)
                .email(user.getPrimaryEmail())
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
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new NotioException(ErrorCode.INVALID_TOKEN);
        }

        final RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedAtIsNull(request.getRefreshToken())
                .orElseThrow(() -> new NotioException(ErrorCode.INVALID_TOKEN));

        if (!refreshToken.isValid()) {
            throw new NotioException(ErrorCode.EXPIRED_TOKEN);
        }

        final User user = refreshToken.getUser();
        final String userId = String.valueOf(user.getId());

        final String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getPrimaryEmail());
        final String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(userId);

        refreshToken.revoke();

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
        final User user = userRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new NotioException(ErrorCode.UNAUTHORIZED));

        refreshTokenRepository.revokeAllByUser(user);

        log.info("User logged out successfully: userId={}", userId);
    }
}
