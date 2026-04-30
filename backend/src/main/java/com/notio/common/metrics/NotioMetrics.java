package com.notio.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Component;

@Component
public class NotioMetrics {

    private final MeterRegistry meterRegistry;
    private final NotioMetricsTagPolicy tagPolicy;

    public NotioMetrics(final MeterRegistry meterRegistry, final NotioMetricsTagPolicy tagPolicy) {
        this.meterRegistry = meterRegistry;
        this.tagPolicy = tagPolicy;
    }

    public void incrementCounter(final String metricName, final Tags tags) {
        Counter.builder(requireNotioMetricName(metricName))
                .tags(sanitize(tags))
                .register(meterRegistry)
                .increment();
    }

    public void recordTimer(final String metricName, final Tags tags, final Duration duration) {
        Timer.builder(requireNotioMetricName(metricName))
                .tags(sanitize(tags))
                .register(meterRegistry)
                .record(duration);
    }

    public void recordSummary(final String metricName, final Tags tags, final long amount, final String baseUnit) {
        DistributionSummary.builder(requireNotioMetricName(metricName))
                .tags(sanitize(tags))
                .baseUnit(baseUnit)
                .register(meterRegistry)
                .record(amount);
    }

    public <T> void registerGauge(
            final String metricName,
            final T stateObject,
            final ToDoubleFunction<T> valueFunction,
            final String description
    ) {
        Gauge.builder(requireNotioMetricName(metricName), stateObject, valueFunction)
                .description(description)
                .register(meterRegistry);
    }

    private String requireNotioMetricName(final String metricName) {
        final String candidate = Objects.requireNonNull(metricName, "metricName must not be null");
        if (!candidate.startsWith("notio_")) {
            throw new IllegalArgumentException("Notio custom metrics must use the 'notio_' prefix.");
        }
        return candidate;
    }

    private Iterable<Tag> sanitize(final Tags tags) {
        return tagPolicy.sanitize(tags);
    }
}
