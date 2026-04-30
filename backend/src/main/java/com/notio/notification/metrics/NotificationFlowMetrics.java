package com.notio.notification.metrics;

import com.notio.common.metrics.NotioMetrics;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NotificationFlowMetrics {

    private final NotioMetrics notioMetrics;

    public NotificationFlowMetrics(final NotioMetrics notioMetrics) {
        this.notioMetrics = notioMetrics;
    }

    public void recordWebhookRequest(final String source, final String outcome, final Duration duration) {
        notioMetrics.incrementCounter("notio_webhook_requests_total", Tags.of("source", source, "outcome", outcome));
        notioMetrics.recordTimer("notio_webhook_processing_duration", Tags.of("source", source), duration);
    }

    public void recordNotificationCreated(final String source) {
        notioMetrics.incrementCounter("notio_notifications_created_total", Tags.of("source", source));
    }

    public void recordNotificationEmbedding(final String outcome) {
        notioMetrics.incrementCounter("notio_notification_embedding_total", Tags.of("outcome", outcome));
    }

    public void recordPushSend(final String outcome, final Duration duration) {
        notioMetrics.incrementCounter("notio_push_send_total", Tags.of("outcome", outcome));
        notioMetrics.recordTimer("notio_push_send_duration", Tags.empty(), duration);
    }
}
