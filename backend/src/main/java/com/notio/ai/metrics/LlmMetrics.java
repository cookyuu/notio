package com.notio.ai.metrics;

import com.notio.common.metrics.NotioMetrics;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class LlmMetrics {

    private final NotioMetrics notioMetrics;

    public LlmMetrics(final NotioMetrics notioMetrics) {
        this.notioMetrics = notioMetrics;
    }

    public void recordLlmCall(final String outcome, final Duration duration) {
        notioMetrics.incrementCounter("notio_llm_call_total", Tags.of("outcome", outcome));
        notioMetrics.recordTimer("notio_llm_call_duration", Tags.of("outcome", outcome), duration);
    }
}
