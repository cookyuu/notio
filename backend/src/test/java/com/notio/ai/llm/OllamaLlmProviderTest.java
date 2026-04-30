package com.notio.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.notio.ai.prompt.LlmPrompt;
import com.notio.chat.metrics.ChatMetrics;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

class OllamaLlmProviderTest {

    @Test
    void chatRecordsSuccessfulLlmMetric() {
        final ChatModel chatModel = mock(ChatModel.class);
        final AiExceptionTranslator exceptionTranslator = mock(AiExceptionTranslator.class);
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        final OllamaLlmProvider provider = new OllamaLlmProvider(
                chatModel,
                exceptionTranslator,
                aiProperties(),
                new ChatMetrics(meterRegistry)
        );
        final ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("응답입니다.");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        final String result = provider.chat(new LlmPrompt("system", "user"));

        assertThat(result).isEqualTo("응답입니다.");
        assertThat(meterRegistry.get("notio_llm_call_duration")
                .tag("mode", "sync")
                .tag("outcome", "success")
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void streamRecordsCancellationMetricAndRethrows() {
        final ChatModel chatModel = mock(ChatModel.class);
        final AiExceptionTranslator exceptionTranslator = mock(AiExceptionTranslator.class);
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        final OllamaLlmProvider provider = new OllamaLlmProvider(
                chatModel,
                exceptionTranslator,
                aiProperties(),
                new ChatMetrics(meterRegistry)
        );
        final ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("첫 청크");
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response));
        final AtomicReference<String> chunk = new AtomicReference<>();

        assertThatThrownBy(() -> provider.stream(
                new LlmPrompt("system", "user"),
                value -> {
                    chunk.set(value);
                    throw new CancellationException("client disconnected");
                }
        )).isInstanceOf(CancellationException.class);

        assertThat(chunk.get()).isEqualTo("첫 청크");
        assertThat(meterRegistry.get("notio_llm_call_duration")
                .tag("mode", "stream")
                .tag("outcome", "cancelled")
                .timer()
                .count()).isEqualTo(1);
    }

    private NotioAiProperties aiProperties() {
        return new NotioAiProperties(
                Duration.ofSeconds(30),
                Duration.ofSeconds(15),
                Duration.ofSeconds(5)
        );
    }
}
