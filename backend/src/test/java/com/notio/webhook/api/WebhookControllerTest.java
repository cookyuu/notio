package com.notio.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.api.ApiResponse;
import com.notio.common.error.GlobalExceptionHandler;
import com.notio.common.error.NotioException;
import com.notio.notification.application.NotificationIngestionService;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.application.WebhookDispatcher;
import com.notio.webhook.application.WebhookRequestContext;
import com.notio.webhook.application.WebhookVerifier;
import com.notio.webhook.infrastructure.SlackWebhookHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class WebhookControllerTest {

    @Test
    void receiveWebhookReturnsUnauthorizedWhenSignatureIsInvalid() {
        final WebhookController controller = new WebhookController(
                new WebhookDispatcher(List.of(new SlackWebhookHandler()), List.of(new SlackFailingVerifier())),
                mock(NotificationIngestionService.class),
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
                    assertThat(response.getBody().error().code()).isEqualTo("UNAUTHORIZED");
                });
    }

    private static final class SlackFailingVerifier implements WebhookVerifier {
        @Override
        public NotificationSource supports() {
            return NotificationSource.SLACK;
        }

        @Override
        public boolean verify(final WebhookRequestContext context) {
            return false;
        }
    }
}
