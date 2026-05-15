package com.notio.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.notio.ai.metrics.LlmMetrics;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import reactor.core.publisher.Flux;

class OllamaLlmProviderTest {

    @Test
    void chatReturnsLlmResponseOnSuccess() {
        OllamaChatModel chatModel = mock(OllamaChatModel.class, RETURNS_DEEP_STUBS);
        OllamaLlmProvider provider = provider(chatModel, Duration.ofSeconds(30));
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("LLM 응답입니다.");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        String result = provider.chat(new LlmPrompt("system", "user"));

        assertThat(result).isEqualTo("LLM 응답입니다.");
    }

    @Test
    void chatRecordsSuccessMetric() {
        OllamaChatModel chatModel = mock(OllamaChatModel.class, RETURNS_DEEP_STUBS);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OllamaLlmProvider provider = provider(chatModel, meterRegistry, Duration.ofSeconds(30));
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("응답");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        provider.chat(new LlmPrompt("system", "user"));

        assertThat(meterRegistry.get("notio_llm_call_total")
            .tag("outcome", "success")
            .counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notio_llm_call_duration")
            .tag("outcome", "success")
            .timer().count()).isEqualTo(1);
    }

    @Test
    void chatThrowsNotioExceptionWhenLlmTimesOut() {
        OllamaChatModel chatModel = mock(OllamaChatModel.class, RETURNS_DEEP_STUBS);
        OllamaLlmProvider provider = provider(chatModel, Duration.ofMillis(50));

        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            Thread.sleep(500);
            return mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        });

        assertThatThrownBy(() -> provider.chat(new LlmPrompt("system", "user")))
            .isInstanceOf(NotioException.class)
            .satisfies(ex -> assertThat(((NotioException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LLM_UNAVAILABLE));
    }

    @Test
    void chatRecordsFailureMetricOnTimeout() {
        OllamaChatModel chatModel = mock(OllamaChatModel.class, RETURNS_DEEP_STUBS);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OllamaLlmProvider provider = provider(chatModel, meterRegistry, Duration.ofMillis(50));

        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            Thread.sleep(500);
            return mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        });

        try {
            provider.chat(new LlmPrompt("system", "user"));
        } catch (NotioException ignored) {}

        assertThat(meterRegistry.get("notio_llm_call_total")
            .tag("outcome", "failure")
            .counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notio_llm_call_duration")
            .tag("outcome", "failure")
            .timer().count()).isEqualTo(1);
    }

    @Test
    void streamRecordsCancellationMetricAndRethrows() {
        OllamaChatModel chatModel = mock(OllamaChatModel.class, RETURNS_DEEP_STUBS);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OllamaLlmProvider provider = provider(chatModel, meterRegistry, Duration.ofSeconds(30));
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("첫 청크");
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response));
        AtomicReference<String> chunk = new AtomicReference<>();

        assertThatThrownBy(() -> provider.stream(
            new LlmPrompt("system", "user"),
            value -> {
                chunk.set(value);
                throw new java.util.concurrent.CancellationException("client disconnected");
            }
        )).isInstanceOf(java.util.concurrent.CancellationException.class);

        assertThat(chunk.get()).isEqualTo("첫 청크");
        assertThat(meterRegistry.get("notio_llm_call_total")
            .tag("outcome", "cancelled")
            .counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notio_llm_call_duration")
            .tag("outcome", "cancelled")
            .timer().count()).isEqualTo(1);
    }

    private OllamaLlmProvider provider(OllamaChatModel chatModel, Duration llmTimeout) {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        return provider(chatModel, meterRegistry, llmTimeout);
    }

    private OllamaLlmProvider provider(OllamaChatModel chatModel, SimpleMeterRegistry meterRegistry, Duration llmTimeout) {
        LlmMetrics llmMetrics = new LlmMetrics(new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy()));
        NotioAiProperties aiProperties = new NotioAiProperties(
            "ollama", llmTimeout, Duration.ofSeconds(10), Duration.ofSeconds(30), List.of("CLAUDE")
        );
        return new OllamaLlmProvider(chatModel, new AiExceptionTranslator(), aiProperties, llmMetrics);
    }
}
