package com.notio.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.GlobalExceptionHandler;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import com.notio.connection.adapter.ConnectionProviderAdapter;
import com.notio.connection.adapter.ConnectionProviderAdapterRegistry;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.service.ConnectionService;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.service.NotificationService;
import com.notio.webhook.dispatcher.WebhookDispatcher;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookRequestContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class WebhookControllerTest {

    @Test
    void receiveWebhookReturnsUnauthorizedWhenSignatureIsInvalid() {
        final WebhookController controller = new WebhookController(
                new WebhookDispatcher(
                    List.of(),
                    List.of(),
                    new ConnectionProviderAdapterRegistry(List.of(new SlackFailingAdapter()))
                ),
                mock(NotificationService.class),
                mock(ConnectionService.class),
                new ObjectMapper()
        );

        assertThatThrownBy(() -> controller.receiveWebhook(
                "slack",
                new HttpHeaders(),
                "{\"text\":\"hello\"}"
        )).isInstanceOf(NotioException.class)
                .satisfies(exception -> {
                    final ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler()
                            .handleNotioException((NotioException) exception);
                    assertThat(response.getStatusCode().value()).isEqualTo(401);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().error().code()).isEqualTo("WEBHOOK_VERIFICATION_FAILED");
                });
    }

    private static final class SlackFailingAdapter implements ConnectionProviderAdapter {
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
            throw new NotioException(com.notio.common.exception.ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        @Override
        public NotificationEvent toNotificationEvent(final WebhookRequestContext context) {
            return new NotificationEvent(
                com.notio.notification.domain.NotificationSource.SLACK,
                "Slack",
                context.rawBody(),
                NotificationPriority.MEDIUM,
                null,
                null,
                context.payload()
            );
        }
    }
}
