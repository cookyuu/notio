package com.notio.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.GlobalExceptionHandler;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.service.NotificationService;
import com.notio.webhook.dispatcher.WebhookDispatcher;
import com.notio.webhook.dto.WebhookRequestContext;
import com.notio.webhook.handler.SlackWebhookHandler;
import com.notio.webhook.verifier.WebhookVerifier;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class WebhookControllerTest {

    @Test
    void receiveWebhookReturnsUnauthorizedWhenSignatureIsInvalid() {
        final WebhookController controller = new WebhookController(
                new WebhookDispatcher(List.of(new SlackWebhookHandler()), List.of(new SlackFailingVerifier())),
                mock(NotificationService.class),
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
