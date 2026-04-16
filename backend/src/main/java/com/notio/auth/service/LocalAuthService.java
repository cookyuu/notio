package com.notio.auth.service;

import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.PasswordResetToken;
import com.notio.auth.domain.User;
import com.notio.auth.domain.UserStatus;
import com.notio.auth.dto.FindIdRequest;
import com.notio.auth.dto.FindIdResponse;
import com.notio.auth.dto.PasswordResetConfirmRequest;
import com.notio.auth.dto.PasswordResetConfirmResponse;
import com.notio.auth.dto.PasswordResetRequestRequest;
import com.notio.auth.dto.PasswordResetRequestResponse;
import com.notio.auth.dto.SignupRequest;
import com.notio.auth.dto.SignupResponse;
import com.notio.auth.repository.AuthIdentityRepository;
import com.notio.auth.repository.PasswordResetTokenRepository;
import com.notio.auth.repository.RefreshTokenRepository;
import com.notio.auth.repository.UserRepository;
import com.notio.auth.util.AuthTokenUtils;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.time.OffsetDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalAuthService {

    private static final String FIND_ID_MESSAGE = "입력한 이메일로 계정 안내를 전송할 예정입니다.";
    private static final String PASSWORD_RESET_REQUEST_MESSAGE = "비밀번호 재설정 안내를 전송할 예정입니다.";
    private static final String PASSWORD_RESET_CONFIRM_MESSAGE = "비밀번호가 재설정되었습니다.";
    private static final long PASSWORD_RESET_TOKEN_TTL_MINUTES = 30L;

    private final UserRepository userRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(final SignupRequest request) {
        final String normalizedEmail = normalizeEmail(request.getEmail());
        if (authIdentityRepository.existsActiveLocalByEmail(normalizedEmail)) {
            throw new NotioException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        final User user = userRepository.save(User.builder()
                .primaryEmail(normalizedEmail)
                .displayName(request.getDisplayName().trim())
                .status(UserStatus.ACTIVE)
                .build());

        authIdentityRepository.save(AuthIdentity.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .build());

        log.info("Local signup completed: userId={}, email={}", user.getId(), normalizedEmail);

        return SignupResponse.builder()
                .userId(String.valueOf(user.getId()))
                .email(user.getPrimaryEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    public FindIdResponse findId(final FindIdRequest request) {
        final String normalizedEmail = normalizeEmail(request.getEmail());
        authIdentityRepository.findActiveLocalByEmail(normalizedEmail)
                .ifPresent(authIdentity -> log.info("Find-id requested for local account: userId={}", authIdentity.getUser().getId()));
        return FindIdResponse.builder()
                .message(FIND_ID_MESSAGE)
                .build();
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(final PasswordResetRequestRequest request) {
        final String normalizedEmail = normalizeEmail(request.getEmail());
        authIdentityRepository.findActiveLocalByEmail(normalizedEmail).ifPresent(authIdentity -> {
            passwordResetTokenRepository.invalidateUnusedByAuthIdentityId(authIdentity.getId());

            final String rawToken = AuthTokenUtils.generateRawToken();
            final String tokenHash = AuthTokenUtils.sha256Hex(rawToken);

            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .user(authIdentity.getUser())
                    .authIdentity(authIdentity)
                    .tokenHash(tokenHash)
                    .expiresAt(OffsetDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_TTL_MINUTES))
                    .build());

            log.info("Password reset requested for userId={}", authIdentity.getUser().getId());
        });

        return PasswordResetRequestResponse.builder()
                .message(PASSWORD_RESET_REQUEST_MESSAGE)
                .build();
    }

    @Transactional
    public PasswordResetConfirmResponse confirmPasswordReset(final PasswordResetConfirmRequest request) {
        final String tokenHash = AuthTokenUtils.sha256Hex(request.getToken().trim());
        final PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new NotioException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        if (passwordResetToken.getUsedAt() != null) {
            throw new NotioException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        if (passwordResetToken.isExpired()) {
            throw new NotioException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        final AuthIdentity authIdentity = passwordResetToken.getAuthIdentity();
        authIdentity.updatePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        passwordResetToken.markUsed();
        refreshTokenRepository.revokeAllByUser(passwordResetToken.getUser());

        log.info("Password reset confirmed for userId={}", passwordResetToken.getUser().getId());

        return PasswordResetConfirmResponse.builder()
                .message(PASSWORD_RESET_CONFIRM_MESSAGE)
                .build();
    }

    private String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
