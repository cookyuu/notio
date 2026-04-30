package com.notio.chat.metrics;

import com.notio.common.metrics.NotioMetrics;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ChatMetrics {

    private final NotioMetrics notioMetrics;
    private final AtomicInteger activeStreams = new AtomicInteger();

    public ChatMetrics(final NotioMetrics notioMetrics) {
        this.notioMetrics = notioMetrics;
        notioMetrics.registerGauge(
                "notio_chat_stream_active",
                activeStreams,
                AtomicInteger::get,
                "Current active chat streams"
        );
    }

    public void incrementActiveStreams() {
        activeStreams.incrementAndGet();
    }

    public void decrementActiveStreams() {
        activeStreams.updateAndGet(current -> Math.max(0, current - 1));
    }

    public void recordChatRequest(final String mode, final String outcome, final Duration duration) {
        notioMetrics.incrementCounter("notio_chat_requests_total", Tags.of("mode", mode, "outcome", outcome));
        notioMetrics.recordTimer("notio_chat_duration", Tags.of("mode", mode), duration);
    }

    public void recordFirstChunk(final Duration duration) {
        notioMetrics.recordTimer("notio_chat_first_chunk_duration", Tags.empty(), duration);
    }

    public void recordResponseChars(final String mode, final long responseChars) {
        notioMetrics.recordSummary("notio_chat_response_chars", Tags.of("mode", mode), responseChars, "characters");
    }

    public void recordLlmCall(final String mode, final String outcome, final Duration duration) {
        notioMetrics.recordTimer("notio_llm_call_duration", Tags.of("mode", mode, "outcome", outcome), duration);
    }

    public void recordRagRetrieval(final boolean timeRangeApplied, final Duration duration) {
        notioMetrics.incrementCounter(
                "notio_rag_retrieval_total",
                Tags.of("time_range_applied", String.valueOf(timeRangeApplied))
        );
        notioMetrics.recordTimer("notio_rag_retrieval_duration", Tags.empty(), duration);
    }
}
