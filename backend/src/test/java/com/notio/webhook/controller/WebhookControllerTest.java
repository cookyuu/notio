package com.notio.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.common.exception.GlobalExceptionHandler;
import com.notio.common.exception.NotioException;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import com.notio.common.response.ApiResponse;
import com.notio.connection.adapter.ConnectionProviderAdapter;
import com.notio.connection.adapter.ConnectionProviderAdapterRegistry;
import com.notio.connection.domain.ConnectionAuthType;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.service.ConnectionService;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.service.NotificationService;
import com.notio.webhook.dispatcher.WebhookDispatcher;
import com.notio.webhook.dto.NotificationEvent;
import com.notio.webhook.dto.WebhookDispatchResult;
import com.notio.webhook.dto.WebhookPrincipal;
import com.notio.webhook.dto.WebhookReceiptResponse;
import com.notio.webhook.dto.WebhookRequestContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class WebhookControllerTest {

    @Test
    void receiveWebhookSavesNotificationAndRecordsSuccess() {
        final WebhookDispatcher webhookDispatcher = mock(WebhookDispatcher.class);
        final NotificationService notificationService = mock(NotificationService.class);
        final ConnectionService connectionService = mock(ConnectionService.class);
        final WebhookController controller = new WebhookController(
            webhookDispatcher,
            notificationService,
            connectionService,
            new ObjectMapper(),
            new NotificationFlowMetrics(new NotioMetrics(new SimpleMeterRegistry(), new NotioMetricsTagPolicy()))
        );
        final NotificationEvent event = new NotificationEvent(
            com.notio.notification.domain.NotificationSource.CLAUDE,
            "Claude alert",
            "body",
            NotificationPriority.HIGH,
            "ext-1",
            null,
            Map.of("scope", "repo"),
            10L,
            20L
        );
        final WebhookPrincipal principal = new WebhookPrincipal(20L, 10L, ConnectionProvider.CLAUDE);
        final Notification notification = Notification.builder()
            .id(99L)
            .userId(10L)
            .connectionId(20L)
            .source(event.source())
            .title(event.title())
            .body(event.body())
            .priority(event.priority())
            .build();
        when(webhookDispatcher.dispatch(org.mockito.ArgumentMatchers.any(WebhookRequestContext.class)))
            .thenReturn(new WebhookDispatchResult(event, principal));
        when(notificationService.saveFromEvent(event)).thenReturn(notification);
        final Logger logger = (Logger) LoggerFactory.getLogger(WebhookController.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final ApiResponse<WebhookReceiptResponse> response;
        try {
            response = controller.receiveWebhook(
                "claude",
                new HttpHeaders(),
                "{\"content\":\"hello\"}"
            );
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().notificationId()).isEqualTo(99L);
        assertThat(response.data().processedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anySatisfy(message -> assertThat(message).contains("event=webhook_received"))
            .anySatisfy(message -> assertThat(message).contains("event=webhook_processed"));
        verify(notificationService).saveFromEvent(event);
        verify(connectionService).recordWebhookSuccess(10L, 20L);
    }

    @Test
    void receiveWebhookPreservesCorrelationIdAcrossReceiveAuthenticateAndProcessLogsWithoutLeakingPayload() {
        final WebhookDispatcher webhookDispatcher = mock(WebhookDispatcher.class);
        final NotificationService notificationService = mock(NotificationService.class);
        final ConnectionService connectionService = mock(ConnectionService.class);
        final WebhookController controller = new WebhookController(
                webhookDispatcher,
                notificationService,
                connectionService,
                new ObjectMapper(),
                new NotificationFlowMetrics(new NotioMetrics(new SimpleMeterRegistry(), new NotioMetricsTagPolicy()))
        );
        final NotificationEvent event = new NotificationEvent(
                com.notio.notification.domain.NotificationSource.CLAUDE,
                "Claude alert",
                "body",
                NotificationPriority.HIGH,
                "ext-1",
                null,
                Map.of("scope", "repo"),
                10L,
                20L
        );
        final WebhookPrincipal principal = new WebhookPrincipal(20L, 10L, ConnectionProvider.CLAUDE);
        final Notification notification = Notification.builder()
                .id(99L)
                .userId(10L)
                .connectionId(20L)
                .source(event.source())
                .title(event.title())
                .body(event.body())
                .priority(event.priority())
                .build();
        when(webhookDispatcher.dispatch(org.mockito.ArgumentMatchers.any(WebhookRequestContext.class)))
                .thenAnswer(invocation -> {
                    MDC.put("event", "webhook_authenticated");
                    MDC.put("outcome", "success");
                    try {
                        ((Logger) LoggerFactory.getLogger(WebhookDispatcher.class)).info(
                                "event=webhook_authenticated source=claude provider=claude user_id=10 connection_id=20"
                        );
                    } finally {
                        MDC.remove("outcome");
                        MDC.remove("event");
                    }
                    return new WebhookDispatchResult(event, principal);
                });
        when(notificationService.saveFromEvent(event)).thenReturn(notification);

        final Logger controllerLogger = (Logger) LoggerFactory.getLogger(WebhookController.class);
        final Logger dispatcherLogger = (Logger) LoggerFactory.getLogger(WebhookDispatcher.class);
        final Level previousControllerLevel = controllerLogger.getLevel();
        final Level previousDispatcherLevel = dispatcherLogger.getLevel();
        final ListAppender<ILoggingEvent> controllerAppender = new ListAppender<>();
        final ListAppender<ILoggingEvent> dispatcherAppender = new ListAppender<>();
        controllerAppender.start();
        dispatcherAppender.start();
        controllerLogger.setLevel(Level.INFO);
        dispatcherLogger.setLevel(Level.INFO);
        controllerLogger.addAppender(controllerAppender);
        dispatcherLogger.addAppender(dispatcherAppender);
        MDC.put("correlation_id", "corr-webhook-success");

        try {
            controller.receiveWebhook(
                    "claude",
                    new HttpHeaders(),
                    "{\"token\":\"super-secret-token\",\"message\":\"hello\"}"
            );
        } finally {
            MDC.clear();
            controllerLogger.detachAppender(controllerAppender);
            dispatcherLogger.detachAppender(dispatcherAppender);
            controllerLogger.setLevel(previousControllerLevel);
            dispatcherLogger.setLevel(previousDispatcherLevel);
            controllerAppender.stop();
            dispatcherAppender.stop();
        }

        final List<ILoggingEvent> events = new ArrayList<>(controllerAppender.list);
        events.addAll(dispatcherAppender.list);
        assertThat(events)
                .extracting(eventLog -> eventLog.getMDCPropertyMap().get("correlation_id"))
                .containsOnly("corr-webhook-success");
        assertThat(events)
                .extracting(eventLog -> eventLog.getMDCPropertyMap().get("event"))
                .contains("webhook_received", "webhook_authenticated", "webhook_processed");
        assertThat(events)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> {
                    assertThat(message).doesNotContain("super-secret-token");
                    assertThat(message).doesNotContain("\"message\":\"hello\"");
                });
    }

    @Test
    void receiveWebhookReturnsUnauthorizedWhenSignatureIsInvalid() {
        final NotificationService notificationService = mock(NotificationService.class);
        final ConnectionService connectionService = mock(ConnectionService.class);
        final WebhookController controller = new WebhookController(
                new WebhookDispatcher(
                    List.of(),
                    List.of(),
                    new ConnectionProviderAdapterRegistry(List.of(new SlackFailingAdapter()))
                ),
                notificationService,
                connectionService,
                new ObjectMapper(),
                new NotificationFlowMetrics(new NotioMetrics(new SimpleMeterRegistry(), new NotioMetricsTagPolicy()))
        );

        assertThatThrownBy(() -> controller.receiveWebhook(
                "slack",
                new HttpHeaders(),
                "{\"text\":\"hello\"}"
        )).isInstanceOf(NotioException.class)
                .satisfies(exception -> {
                    final ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler()
                            .handleNotioException((NotioException) exception, new MockHttpServletRequest("POST", "/api/v1/webhook/slack"));
                    assertThat(response.getStatusCode().value()).isEqualTo(401);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().error().code()).isEqualTo("WEBHOOK_VERIFICATION_FAILED");
                });

        verify(notificationService, never()).saveFromEvent(org.mockito.ArgumentMatchers.any());
        verify(connectionService, never()).recordWebhookSuccess(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void receiveWebhookDoesNotSaveNotificationWhenGmailVerificationFails() {
        final NotificationService notificationService = mock(NotificationService.class);
        final ConnectionService connectionService = mock(ConnectionService.class);
        final WebhookController controller = new WebhookController(
            new WebhookDispatcher(
                List.of(),
                List.of(),
                new ConnectionProviderAdapterRegistry(List.of(new GmailFailingAdapter()))
            ),
            notificationService,
            connectionService,
            new ObjectMapper(),
            new NotificationFlowMetrics(new NotioMetrics(new SimpleMeterRegistry(), new NotioMetricsTagPolicy()))
        );

        assertThatThrownBy(() -> controller.receiveWebhook(
            "gmail",
            new HttpHeaders(),
            "{\"message\":\"hello\"}"
        )).isInstanceOf(NotioException.class)
            .hasMessage("Webhook 서명 검증에 실패했습니다.");

        verify(notificationService, never()).saveFromEvent(org.mockito.ArgumentMatchers.any());
        verify(connectionService, never()).recordWebhookSuccess(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
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

    private static final class GmailFailingAdapter implements ConnectionProviderAdapter {
        @Override
        public ConnectionProvider supports() {
            return ConnectionProvider.GMAIL;
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
                com.notio.notification.domain.NotificationSource.GMAIL,
                "Gmail",
                context.rawBody(),
                NotificationPriority.MEDIUM,
                null,
                null,
                context.payload()
            );
        }
    }
}
