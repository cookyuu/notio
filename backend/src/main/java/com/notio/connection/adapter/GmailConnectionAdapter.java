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
public class GmailConnectionAdapter implements ConnectionProviderAdapter {

    @Override
    public ConnectionProvider supports() {
        return ConnectionProvider.GMAIL;
    }

    @Override
    public boolean supportsAuthType(final ConnectionAuthType authType) {
        return authType == ConnectionAuthType.OAUTH;
    }

    @Override
    public WebhookPrincipal authenticateWebhook(final WebhookRequestContext context) {
        // Phase 2 skeleton: verify Pub/Sub/OIDC/subscription and match by subscription_id or account id.
        throw new NotioException(ErrorCode.CONNECTION_PROVIDER_UNSUPPORTED);
    }

    public String buildOAuthUrl(final String state, final String redirectUri) {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("state", state)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .build()
            .toUriString();
    }

    public void handleOAuthCallback(final MultiValueMap<String, String> parameters) {
        // Phase 2 skeleton: exchange code, store provider account/email and encrypted OAuth tokens.
        throw new NotioException(ErrorCode.CONNECTION_PROVIDER_UNSUPPORTED);
    }

    public void createOrRefreshSubscription(final Long connectionId) {
        // Phase 2 skeleton: create or renew Gmail Pub/Sub watch subscription.
        throw new NotioException(ErrorCode.CONNECTION_PROVIDER_UNSUPPORTED);
    }

    public boolean verifyPubSubRequest(final WebhookRequestContext context) {
        // Phase 2 skeleton: verify Google Pub/Sub/OIDC/subscription before matching a connection.
        return false;
    }

    @Override
    public NotificationEvent toNotificationEvent(final WebhookRequestContext context) {
        return new NotificationEvent(NotificationSource.GMAIL, "Gmail notification", context.rawBody(), com.notio.notification.domain.NotificationPriority.MEDIUM, null, null, context.payload());
    }
}
