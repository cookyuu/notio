package com.notio.auth.service;

import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import com.notio.auth.support.AuthMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthAuditService {

    private static final String EVENT_KEY = "event";
    private static final String OUTCOME_KEY = "outcome";

    public void recordSignupSuccess(final Long userId, final String email) {
        withMdc("auth_signup_succeeded", "success", () -> log.info(
                "event=auth_signup_succeeded outcome=success provider={} user_id={} masked_email={}",
                AuthProvider.LOCAL,
                userId,
                AuthMaskingUtils.maskEmail(email)
        ));
    }

    public void recordFindIdRequested(final String email, final boolean accountFound) {
        withMdc("auth_find_id_requested", "accepted", () -> log.info(
                "event=auth_find_id_requested outcome=accepted provider={} masked_email={} account_found={}",
                AuthProvider.LOCAL,
                AuthMaskingUtils.maskEmail(email),
                accountFound
        ));
    }

    public void recordPasswordResetRequested(final Long userId, final String email, final boolean accountFound) {
        withMdc("auth_password_reset_requested", "accepted", () -> log.info(
                "event=auth_password_reset_requested outcome=accepted provider={} user_id={} masked_email={} account_found={}",
                AuthProvider.LOCAL,
                userId,
                AuthMaskingUtils.maskEmail(email),
                accountFound
        ));
    }

    public void recordPasswordResetConfirmed(final Long userId) {
        withMdc("auth_password_reset_confirmed", "success", () -> log.info(
                "event=auth_password_reset_confirmed outcome=success provider={} user_id={}",
                AuthProvider.LOCAL,
                userId
        ));
    }

    public void recordPasswordResetFailed(final String reason) {
        withMdc("auth_password_reset_failed", "failure", () -> log.warn(
                "event=auth_password_reset_failed outcome=failure provider={} reason_category={}",
                AuthProvider.LOCAL,
                reason
        ));
    }

    public void recordOAuthStart(final AuthProvider provider, final AuthPlatform platform) {
        withMdc("oauth_start", "success", () -> log.info(
                "event=oauth_start outcome=success provider={} platform={}",
                provider,
                platform
        ));
    }

    public void recordOAuthCallbackSuccess(final AuthProvider provider, final AuthPlatform platform) {
        withMdc("oauth_callback_validated", "success", () -> log.info(
                "event=oauth_callback_validated outcome=success provider={} platform={}",
                provider,
                platform
        ));
    }

    public void recordOAuthCallbackFailure(
            final AuthProvider provider,
            final AuthPlatform platform,
            final String reason
    ) {
        withMdc("oauth_callback_validated", "failure", () -> log.warn(
                "event=oauth_callback_validated outcome=failure provider={} platform={} reason_category={}",
                provider,
                platform,
                reason
        ));
    }

    public void recordOAuthExchangeSucceeded(final AuthProvider provider, final AuthPlatform platform) {
        withMdc("oauth_exchange_succeeded", "success", () -> log.info(
                "event=oauth_exchange_succeeded outcome=success provider={} platform={}",
                provider,
                platform
        ));
    }

    public void recordOAuthExchangeFailed(
            final AuthProvider provider,
            final AuthPlatform platform,
            final String reason
    ) {
        withMdc("oauth_exchange_failed", "failure", () -> log.warn(
                "event=oauth_exchange_failed outcome=failure provider={} platform={} reason_category={}",
                provider,
                platform,
                reason
        ));
    }

    private void withMdc(final String event, final String outcome, final Runnable action) {
        MDC.put(EVENT_KEY, event);
        MDC.put(OUTCOME_KEY, outcome);
        try {
            action.run();
        } finally {
            MDC.remove(OUTCOME_KEY);
            MDC.remove(EVENT_KEY);
        }
    }
}
