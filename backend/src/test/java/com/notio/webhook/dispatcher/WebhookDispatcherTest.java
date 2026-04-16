package com.notio.webhook.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notio.common.exception.NotioException;
import com.notio.connection.adapter.ConnectionProviderAdapter;
import com.notio.connection.adapter.ConnectionProviderAdapterRegistry;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookDispatchResult;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookRequestContext;
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
                List.of(),
                List.of(),
                new ConnectionProviderAdapterRegistry(List.of(new TestAdapter(event, true)))
        );

        final WebhookDispatchResult result = dispatcher.dispatch(new WebhookRequestContext(
                NotificationSource.SLACK,
                new HttpHeaders(),
                "{\"text\":\"hello\"}",
                Map.of("text", "hello")
        ));

        assertThat(result.event().title()).isEqualTo(event.title());
        assertThat(result.event().userId()).isEqualTo(10L);
        assertThat(result.event().connectionId()).isEqualTo(20L);
    }

    @Test
    void dispatchThrowsUnauthorizedWhenVerificationFails() {
        final WebhookDispatcher dispatcher = new WebhookDispatcher(
                List.of(),
                List.of(),
                new ConnectionProviderAdapterRegistry(List.of(new TestAdapter(null, false)))
        );

        assertThatThrownBy(() -> dispatcher.dispatch(new WebhookRequestContext(
                NotificationSource.SLACK,
                new HttpHeaders(),
                "{}",
                Map.of()
        ))).isInstanceOf(NotioException.class)
                .hasMessage("Webhook 서명 검증에 실패했습니다.");
    }

    private record TestAdapter(NotificationEvent event, boolean authenticated) implements ConnectionProviderAdapter {
        @Override
        public ConnectionProvider supports() {
            return ConnectionProvider.SLACK;
        }

        @Override
        public boolean supportsAuthType(final ConnectionAuthType authType) {
            return true;
        }

        @Override
        public WebhookPrincipal authenticateWebhook(final WebhookRequestContext context) {
            if (!authenticated) {
                throw new NotioException(com.notio.common.exception.ErrorCode.WEBHOOK_VERIFICATION_FAILED);
            }
            return new WebhookPrincipal(20L, 10L, ConnectionProvider.SLACK);
        }

        @Override
        public NotificationEvent toNotificationEvent(final WebhookRequestContext context) {
            return event;
        }
    }
}

