package com.notio.webhook.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.response.ApiResponse;
import com.notio.connection.service.ConnectionService;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookDispatchResult;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookReceiptResponse;
import com.notio.notification.service.NotificationService;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import com.notio.webhook.dispatcher.WebhookDispatcher;
import com.notio.webhook.dto.WebhookRequestContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WebhookDispatcher webhookDispatcher;
    private final NotificationService notificationService;
    private final ConnectionService connectionService;
    private final ObjectMapper objectMapper;
    private final NotificationFlowMetrics notificationFlowMetrics;

    public WebhookController(
            final WebhookDispatcher webhookDispatcher,
            final NotificationService notificationService,
            final ConnectionService connectionService,
            final ObjectMapper objectMapper,
            final NotificationFlowMetrics notificationFlowMetrics
    ) {
        this.webhookDispatcher = webhookDispatcher;
        this.notificationService = notificationService;
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
        this.notificationFlowMetrics = notificationFlowMetrics;
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
        final Instant startedAt = Instant.now();
        final NotificationSource notificationSource = NotificationSource.from(source);
        logWebhookReceived(notificationSource, rawBody);
        final Map<String, Object> payload = readPayload(rawBody);
        final WebhookRequestContext context = new WebhookRequestContext(
                notificationSource,
                headers,
                rawBody,
                payload
        );
        try {
            final WebhookDispatchResult dispatchResult = webhookDispatcher.dispatch(context);
            final NotificationEvent event = dispatchResult.event();
            final Notification notification = notificationService.saveFromEvent(event);
            final WebhookPrincipal principal = dispatchResult.principal();
            connectionService.recordWebhookSuccess(principal.userId(), principal.connectionId());
            final Instant processedAt = Instant.now();
            logWebhookProcessed(notificationSource, principal, notification, startedAt, processedAt);
            notificationFlowMetrics.recordWebhookRequest(
                    metricTag(notificationSource),
                    "success",
                    Duration.between(startedAt, processedAt)
            );

            return ApiResponse.success(WebhookReceiptResponse.of(notification.getId(), processedAt));
        } catch (RuntimeException exception) {
            logWebhookRejected(notificationSource, exception, startedAt);
            notificationFlowMetrics.recordWebhookRequest(
                    metricTag(notificationSource),
                    "failure",
                    Duration.between(startedAt, Instant.now())
            );
            throw exception;
        }
    }

    private Map<String, Object> readPayload(final String rawBody) {
        try {
            return objectMapper.readValue(rawBody, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("유효한 JSON 본문이 아닙니다.");
        }
    }

    private void logWebhookReceived(final NotificationSource source, final String rawBody) {
        MDC.put("event", "webhook_received");
        MDC.put("outcome", "started");
        try {
            log.info(
                    "event=webhook_received source={} provider={} payload_size={}",
                    metricTag(source),
                    metricTag(source),
                    rawBody.getBytes(StandardCharsets.UTF_8).length
            );
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
    }

    private void logWebhookProcessed(
            final NotificationSource source,
            final WebhookPrincipal principal,
            final Notification notification,
            final Instant startedAt,
            final Instant processedAt
    ) {
        MDC.put("event", "webhook_processed");
        MDC.put("outcome", "success");
        try {
            log.info(
                    "event=webhook_processed source={} provider={} notification_id={} user_id={} connection_id={} elapsed_ms={}",
                    metricTag(source),
                    metricTag(principal.provider().name()),
                    notification.getId(),
                    principal.userId(),
                    principal.connectionId(),
                    Duration.between(startedAt, processedAt).toMillis()
            );
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
    }

    private void logWebhookRejected(
            final NotificationSource source,
            final RuntimeException exception,
            final Instant startedAt
    ) {
        MDC.put("event", "webhook_rejected");
        MDC.put("outcome", "failure");
        try {
            log.warn(
                    "event=webhook_rejected source={} provider={} elapsed_ms={} exception_type={}",
                    metricTag(source),
                    metricTag(source),
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    exception.getClass().getSimpleName()
            );
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
    }

    private String metricTag(final NotificationSource source) {
        return metricTag(source.name());
    }

    private String metricTag(final String value) {
        return value.toLowerCase();
    }
}
