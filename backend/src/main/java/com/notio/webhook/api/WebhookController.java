package com.notio.webhook.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.api.ApiResponse;
import com.notio.notification.application.NotificationEvent;
import com.notio.notification.application.NotificationIngestionService;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.application.WebhookDispatcher;
import com.notio.webhook.application.WebhookRequestContext;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WebhookDispatcher webhookDispatcher;
    private final NotificationIngestionService notificationIngestionService;
    private final ObjectMapper objectMapper;

    public WebhookController(
            final WebhookDispatcher webhookDispatcher,
            final NotificationIngestionService notificationIngestionService,
            final ObjectMapper objectMapper
    ) {
        this.webhookDispatcher = webhookDispatcher;
        this.notificationIngestionService = notificationIngestionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            value = "/{source}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<WebhookReceiptResponse> receiveWebhook(
            @PathVariable final String source,
            @RequestHeader final HttpHeaders headers,
            @RequestBody final String rawBody
    ) {
        final NotificationSource notificationSource = NotificationSource.from(source);
        final Map<String, Object> payload = readPayload(rawBody);
        final WebhookRequestContext context = new WebhookRequestContext(
                notificationSource,
                headers,
                rawBody,
                payload
        );
        final NotificationEvent event = webhookDispatcher.dispatch(context);
        final long notificationId = notificationIngestionService.saveFromEvent(event);
        return ApiResponse.success(new WebhookReceiptResponse(notificationId, true));
    }

    private Map<String, Object> readPayload(final String rawBody) {
        try {
            return objectMapper.readValue(rawBody, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("유효한 JSON 본문이 아닙니다.");
        }
    }
}
