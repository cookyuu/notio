package com.notio.channel.metrics;

import com.notio.common.metrics.NotioMetrics;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ChannelDeliveryMetrics {

    private final NotioMetrics notioMetrics;

    public void recordDelivery(String channelType, String outcome, Duration duration) {
        notioMetrics.incrementCounter(
            "notio_channel_delivery_total",
            Tags.of("channel_type", channelType, "outcome", outcome)
        );
        notioMetrics.recordTimer(
            "notio_channel_delivery_duration",
            Tags.of("channel_type", channelType),
            duration
        );
    }

    public void recordDigestDelivery(String channelType, int notificationCount) {
        notioMetrics.incrementCounter(
            "notio_channel_digest_delivery_total",
            Tags.of("channel_type", channelType)
        );
        notioMetrics.recordSummary(
            "notio_channel_digest_notification_count",
            Tags.of("channel_type", channelType),
            notificationCount,
            "notifications"
        );
    }

    public void recordRetry(String channelType) {
        notioMetrics.incrementCounter(
            "notio_channel_delivery_retry_total",
            Tags.of("channel_type", channelType)
        );
    }

    public void recordDead(String channelType) {
        notioMetrics.incrementCounter(
            "notio_channel_delivery_dead_total",
            Tags.of("channel_type", channelType)
        );
    }
}
