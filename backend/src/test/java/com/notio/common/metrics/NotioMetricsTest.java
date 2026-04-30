package com.notio.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notio.common.config.MetricsConfig;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class NotioMetricsTest {

    @Test
    void customMetricsUseNotioPrefixAndStripHighCardinalityTags() {
        final SimpleMeterRegistry meterRegistry = meterRegistry();
        final NotioMetrics metrics = new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy());

        metrics.incrementCounter(
                "notio_chat_requests_total",
                Tags.of(
                        "mode", "sync",
                        "outcome", "success",
                        "correlation_id", "corr-123",
                        "user_id", "10",
                        "stream_id", "stream-7",
                        "debug", "ignored"
                )
        );

        final Meter.Id meterId = meterRegistry.find("notio_chat_requests_total").counter().getId();

        assertThat(meterId.getTag("mode")).isEqualTo("sync");
        assertThat(meterId.getTag("outcome")).isEqualTo("success");
        assertThat(meterId.getTag("correlation_id")).isNull();
        assertThat(meterId.getTag("user_id")).isNull();
        assertThat(meterId.getTag("stream_id")).isNull();
        assertThat(meterId.getTag("debug")).isNull();
    }

    @Test
    void customMetricsRejectNamesWithoutNotioPrefix() {
        final NotioMetrics metrics = new NotioMetrics(new SimpleMeterRegistry(), new NotioMetricsTagPolicy());

        assertThatThrownBy(() -> metrics.incrementCounter("chat_requests_total", Tags.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notio_");
    }

    @Test
    void meterFilterDoesNotBreakHttpServerMetrics() {
        final SimpleMeterRegistry meterRegistry = meterRegistry();

        meterRegistry.counter("http.server.requests", "uri", "/api/v1/chat", "exception", "none").increment();

        assertThat(meterRegistry.get("http.server.requests")
                .tag("uri", "/api/v1/chat")
                .tag("exception", "none")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    private SimpleMeterRegistry meterRegistry() {
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.config().meterFilter(new MetricsConfig().notioMetricsTagFilter(new NotioMetricsTagPolicy()));
        return meterRegistry;
    }
}
