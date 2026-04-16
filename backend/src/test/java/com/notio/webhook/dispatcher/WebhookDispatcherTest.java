package com.notio.webhook.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notio.common.exception.NotioException;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookRequestContext;
import com.notio.webhook.handler.WebhookHandler;
import com.notio.webhook.verifier.WebhookVerifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class WebhookDispatcherTest {

    @Test
    void dispatchReturnsNotificationEventWhenVerificationSucceeds() {
        final NotificationEvent event = new NotificationEvent(
                NotificationSource.SLACK,
                "Slack alert",
                "body",
                NotificationPriority.HIGH,
                "ext-1",
                null,
                Map.of()
        );
        final WebhookDispatcher dispatcher = new WebhookDispatcher(
                List.of(new TestHandler(event)),
                List.of(new TestVerifier(true))
        );

        final NotificationEvent dispatchedEvent = dispatcher.dispatch(new WebhookRequestContext(
                NotificationSource.SLACK,
                new HttpHeaders(),
                "{\"text\":\"hello\"}",
                Map.of("text", "hello")
        ));

        assertThat(dispatchedEvent).isEqualTo(event);
    }

    @Test
    void dispatchThrowsUnauthorizedWhenVerificationFails() {
        final WebhookDispatcher dispatcher = new WebhookDispatcher(
                List.of(new TestHandler(null)),
                List.of(new TestVerifier(false))
        );

        assertThatThrownBy(() -> dispatcher.dispatch(new WebhookRequestContext(
                NotificationSource.SLACK,
                new HttpHeaders(),
                "{}",
                Map.of()
        ))).isInstanceOf(NotioException.class)
                .hasMessage("Webhook 서명 검증에 실패했습니다.");
    }

    private record TestHandler(NotificationEvent event) implements WebhookHandler {
        @Override
        public NotificationSource supports() {
            return NotificationSource.SLACK;
        }

        @Override
        public NotificationEvent handle(final WebhookRequestContext context) {
            return event;
        }
    }

    private record TestVerifier(boolean result) implements WebhookVerifier {
        @Override
        public NotificationSource supports() {
            return NotificationSource.SLACK;
        }

        @Override
        public boolean verify(final WebhookRequestContext context) {
            return result;
        }
    }
}

