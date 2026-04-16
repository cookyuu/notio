package com.notio.connection.adapter;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookRequestContext;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SlackConnectionAdapter implements ConnectionProviderAdapter {

    @Override
    public ConnectionProvider supports() {
        return ConnectionProvider.SLACK;
    }

    @Override
    public boolean supportsAuthType(final ConnectionAuthType authType) {
        return authType == ConnectionAuthType.OAUTH;
    }

    @Override
    public WebhookPrincipal authenticateWebhook(final WebhookRequestContext context) {
        // Phase 2 skeleton: verify Slack signature, handle URL verification, then match by team_id.
        throw new NotioException(ErrorCode.CONNECTION_PROVIDER_UNSUPPORTED);
    }

    public String buildOAuthUrl(final String state, final String redirectUri) {
        return UriComponentsBuilder.fromUriString("https://slack.com/oauth/v2/authorize")
            .queryParam("state", state)
            .queryParam("redirect_uri", redirectUri)
            .build()
            .toUriString();
    }

    public void handleOAuthCallback(final MultiValueMap<String, String> parameters) {
        // Phase 2 skeleton: exchange code, store team_id/workspace id and encrypted OAuth tokens.
        throw new NotioException(ErrorCode.CONNECTION_PROVIDER_UNSUPPORTED);
    }

    public boolean verifySigningSecret(final WebhookRequestContext context) {
        // Phase 2 skeleton: validate X-Slack-Signature over raw payload.
        return false;
    }

    public String resolveUrlVerificationChallenge(final WebhookRequestContext context) {
        final Object challenge = context.payload().get("challenge");
        return challenge instanceof String value ? value : null;
    }

    @Override
    public NotificationEvent toNotificationEvent(final WebhookRequestContext context) {
        return new NotificationEvent(NotificationSource.SLACK, "Slack notification", context.rawBody(), com.notio.notification.domain.NotificationPriority.MEDIUM, null, null, context.payload());
    }
}
