package com.notio.auth.service;

import com.notio.auth.config.JwtProperties;
import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.RefreshToken;
import com.notio.auth.domain.User;
import com.notio.auth.dto.AuthUserResponse;
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
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String EVENT_KEY = "event";
    private static final String OUTCOME_KEY = "outcome";

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
        final String maskedEmail = AuthMaskingUtils.maskEmail(request.getEmail());
        final AuthIdentity authIdentity = authIdentityRepository
                .findActiveByProviderAndEmail(AuthProvider.LOCAL, request.getEmail())
                .orElseThrow(() -> {
                    logLoginFailure(maskedEmail, "identity_not_found");
                    return new NotioException(ErrorCode.INVALID_CREDENTIALS);
                });
        final User user = authIdentity.getUser();

        if (authIdentity.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), authIdentity.getPasswordHash())) {
            logLoginFailure(maskedEmail, "password_mismatch");
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

        logLoginSuccess(userId, AuthMaskingUtils.maskEmail(user.getPrimaryEmail()));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn((int) (jwtProperties.getExpiration() / 1000))
                .user(buildUserResponse(user))
                .build();
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public RefreshResponse refresh(final RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            logRefreshFailure(null, null, "token_invalid");
            throw new NotioException(ErrorCode.INVALID_TOKEN);
        }

        final RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedAtIsNull(request.getRefreshToken())
                .orElseThrow(() -> {
                    logRefreshFailure(null, null, "refresh_token_not_found");
                    return new NotioException(ErrorCode.INVALID_TOKEN);
                });

        if (!refreshToken.isValid()) {
            final User user = refreshToken.getUser();
            logRefreshFailure(
                    String.valueOf(user.getId()),
                    AuthMaskingUtils.maskEmail(user.getPrimaryEmail()),
                    "refresh_token_expired"
            );
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

        logRefreshSuccess(userId, AuthMaskingUtils.maskEmail(user.getPrimaryEmail()));

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn((int) (jwtProperties.getExpiration() / 1000))
                .user(buildUserResponse(user))
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

    private AuthUserResponse buildUserResponse(final User user) {
        return AuthUserResponse.builder()
                .id(user.getId())
                .primaryEmail(user.getPrimaryEmail())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .build();
    }

    private void logLoginSuccess(final String userId, final String maskedEmail) {
        MDC.put(EVENT_KEY, "auth_login_succeeded");
        MDC.put(OUTCOME_KEY, "success");
        try {
            log.info(
                    "event=auth_login_succeeded outcome=success provider={} user_id={} masked_email={}",
                    AuthProvider.LOCAL,
                    userId,
                    maskedEmail
            );
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }

    private void logLoginFailure(final String maskedEmail, final String reasonCategory) {
        MDC.put(EVENT_KEY, "auth_login_failed");
        MDC.put(OUTCOME_KEY, "failure");
        try {
            log.warn(
                    "event=auth_login_failed outcome=failure provider={} masked_email={} reason_category={}",
                    AuthProvider.LOCAL,
                    maskedEmail,
                    reasonCategory
            );
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }

    private void logRefreshSuccess(final String userId, final String maskedEmail) {
        MDC.put(EVENT_KEY, "auth_refresh_succeeded");
        MDC.put(OUTCOME_KEY, "success");
        try {
            log.info(
                    "event=auth_refresh_succeeded outcome=success provider={} user_id={} masked_email={}",
                    AuthProvider.LOCAL,
                    userId,
                    maskedEmail
            );
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }

    private void logRefreshFailure(
            final String userId,
            final String maskedEmail,
            final String reasonCategory
    ) {
        MDC.put(EVENT_KEY, "auth_refresh_failed");
        MDC.put(OUTCOME_KEY, "failure");
        try {
            log.warn(
                    "event=auth_refresh_failed outcome=failure provider={} user_id={} masked_email={} reason_category={}",
                    AuthProvider.LOCAL,
                    userId,
                    maskedEmail,
                    reasonCategory
            );
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }
}
