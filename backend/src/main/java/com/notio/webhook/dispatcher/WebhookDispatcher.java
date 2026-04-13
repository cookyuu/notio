package com.notio.webhook.dispatcher;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.WebhookRequestContext;
import com.notio.webhook.handler.WebhookHandler;
import com.notio.webhook.verifier.WebhookVerifier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WebhookDispatcher {

    private final Map<NotificationSource, WebhookHandler> handlers;
    private final Map<NotificationSource, WebhookVerifier> verifiers;

    public WebhookDispatcher(
            final List<WebhookHandler> handlers,
            final List<WebhookVerifier> verifiers
    ) {
        this.handlers = toHandlerMap(handlers);
        this.verifiers = toVerifierMap(verifiers);
    }

    public NotificationEvent dispatch(final WebhookRequestContext context) {
        final WebhookHandler handler = handlers.get(context.source());
        if (handler == null) {
            throw new NotioException(ErrorCode.UNSUPPORTED_SOURCE);
        }

        final WebhookVerifier verifier = verifiers.get(context.source());
        if (verifier == null || !verifier.verify(context)) {
            throw new NotioException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        return handler.handle(context);
    }

    private Map<NotificationSource, WebhookHandler> toHandlerMap(final List<WebhookHandler> handlers) {
        final Map<NotificationSource, WebhookHandler> result = new EnumMap<>(NotificationSource.class);
        for (WebhookHandler handler : handlers) {
            result.put(handler.supports(), handler);
        }
        return result;
    }

    private Map<NotificationSource, WebhookVerifier> toVerifierMap(final List<WebhookVerifier> verifiers) {
        final Map<NotificationSource, WebhookVerifier> result = new EnumMap<>(NotificationSource.class);
        for (WebhookVerifier verifier : verifiers) {
            result.put(verifier.supports(), verifier);
        }
        return result;
    }
}
