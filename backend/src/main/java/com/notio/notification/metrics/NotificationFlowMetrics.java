package com.notio.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NotificationFlowMetrics {

    private final MeterRegistry meterRegistry;

    public NotificationFlowMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordWebhookRequest(final String source, final String outcome, final Duration duration) {
        Counter.builder("notio_webhook_requests_total")
                .tag("source", source)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        Timer.builder("notio_webhook_processing_duration")
                .tag("source", source)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordNotificationCreated(final String source) {
        Counter.builder("notio_notifications_created_total")
                .tag("source", source)
                .register(meterRegistry)
                .increment();
    }

    public void recordNotificationEmbedding(final String outcome) {
        Counter.builder("notio_notification_embedding_total")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void recordPushSend(final String outcome, final Duration duration) {
        Counter.builder("notio_push_send_total")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        Timer.builder("notio_push_send_duration")
                .register(meterRegistry)
                .record(duration);
    }
}
