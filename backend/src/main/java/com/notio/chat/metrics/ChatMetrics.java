package com.notio.chat.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ChatMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeStreams = new AtomicInteger();

    public ChatMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("notio_chat_stream_active", activeStreams, AtomicInteger::get)
                .description("Current active chat streams")
                .register(meterRegistry);
    }

    public void incrementActiveStreams() {
        activeStreams.incrementAndGet();
    }

    public void decrementActiveStreams() {
        activeStreams.updateAndGet(current -> Math.max(0, current - 1));
    }

    public void recordChatRequest(final String mode, final String outcome, final Duration duration) {
        Counter.builder("notio_chat_requests_total")
                .tag("mode", mode)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        Timer.builder("notio_chat_duration")
                .tag("mode", mode)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordFirstChunk(final Duration duration) {
        Timer.builder("notio_chat_first_chunk_duration")
                .register(meterRegistry)
                .record(duration);
    }

    public void recordResponseChars(final String mode, final long responseChars) {
        DistributionSummary.builder("notio_chat_response_chars")
                .tag("mode", mode)
                .baseUnit("characters")
                .register(meterRegistry)
                .record(responseChars);
    }

    public void recordLlmCall(final String mode, final String outcome, final Duration duration) {
        Timer.builder("notio_llm_call_duration")
                .tag("mode", mode)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordRagRetrieval(final boolean timeRangeApplied, final Duration duration) {
        Counter.builder("notio_rag_retrieval_total")
                .tag("time_range_applied", String.valueOf(timeRangeApplied))
                .register(meterRegistry)
                .increment();
        Timer.builder("notio_rag_retrieval_duration")
                .register(meterRegistry)
                .record(duration);
    }
}
