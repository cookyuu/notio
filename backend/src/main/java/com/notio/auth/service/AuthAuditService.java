package com.notio.auth.service;

import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.support.AuthMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthAuditService {

    public void recordSignupSuccess(final Long userId, final String email) {
        log.info("AUTH_AUDIT action=signup outcome=success userId={} email={}", userId, AuthMaskingUtils.maskEmail(email));
    }

    public void recordFindIdRequested(final String email, final boolean accountFound) {
        log.info(
                "AUTH_AUDIT action=find_id outcome=accepted email={} accountFound={}",
                AuthMaskingUtils.maskEmail(email),
                accountFound
        );
    }

    public void recordPasswordResetRequested(final Long userId, final String email, final boolean accountFound) {
        log.info(
                "AUTH_AUDIT action=password_reset_request outcome=accepted userId={} email={} accountFound={}",
                userId,
                AuthMaskingUtils.maskEmail(email),
                accountFound
        );
    }

    public void recordPasswordResetConfirmed(final Long userId) {
        log.info("AUTH_AUDIT action=password_reset_confirm outcome=success userId={}", userId);
    }

    public void recordPasswordResetFailed(final String reason) {
        log.warn("AUTH_AUDIT action=password_reset_confirm outcome=failure reason={}", reason);
    }

    public void recordOAuthStart(final AuthProvider provider, final AuthPlatform platform) {
        log.info("AUTH_AUDIT action=oauth_start outcome=success provider={} platform={}", provider, platform);
    }

    public void recordOAuthCallbackSuccess(final AuthProvider provider, final AuthPlatform platform) {
        log.info("AUTH_AUDIT action=oauth_callback outcome=success provider={} platform={}", provider, platform);
    }

    public void recordOAuthCallbackFailure(
            final AuthProvider provider,
            final AuthPlatform platform,
            final String reason
    ) {
        log.warn(
                "AUTH_AUDIT action=oauth_callback outcome=failure provider={} platform={} reason={}",
                provider,
                platform,
                reason
        );
    }
}
