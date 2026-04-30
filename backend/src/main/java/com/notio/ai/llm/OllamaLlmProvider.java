package com.notio.ai.llm;

import com.notio.ai.prompt.LlmPrompt;
import com.notio.chat.metrics.ChatMetrics;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaLlmProvider implements LlmProvider {

    private final ChatModel chatModel;
    private final AiExceptionTranslator exceptionTranslator;
    private final NotioAiProperties aiProperties;
    private final ChatMetrics chatMetrics;

    @Override
    public String chat(final LlmPrompt prompt) {
        final Instant startedAt = Instant.now();
        logLlmStarted("sync", aiProperties.llmTimeout().toMillis());
        try {
            final ChatResponse response = callWithTimeout(prompt);
            final String content = extractContent(response);
            logLlmCompleted("sync", startedAt);
            return content;
        } catch (RuntimeException exception) {
            logLlmFailed("sync", startedAt, exception);
            throw exceptionTranslator.llmUnavailable(exception);
        }
    }

    @Override
    public void stream(final LlmPrompt prompt, final Consumer<String> chunkConsumer) {
        final Instant startedAt = Instant.now();
        logLlmStarted("stream", aiProperties.streamingTimeout().toMillis());
        try {
            chatModel.stream(toSpringPrompt(prompt))
                    .timeout(aiProperties.streamingTimeout())
                    .map(this::extractChunk)
                    .filter(chunk -> !chunk.isBlank())
                    .doOnNext(chunkConsumer)
                    .blockLast(aiProperties.streamingTimeout());
            logLlmCompleted("stream", startedAt);
        } catch (CancellationException exception) {
            logLlmFailed("stream", startedAt, exception);
            throw exception;
        } catch (RuntimeException exception) {
            logLlmFailed("stream", startedAt, exception);
            throw exceptionTranslator.llmUnavailable(exception);
        }
    }

    private Prompt toSpringPrompt(final LlmPrompt prompt) {
        return new Prompt(
                new SystemMessage(prompt.system()),
                new UserMessage(prompt.user())
        );
    }

    private ChatResponse callWithTimeout(final LlmPrompt prompt) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            final Future<ChatResponse> future = executorService.submit(() -> chatModel.call(toSpringPrompt(prompt)));
            try {
                return future.get(aiProperties.llmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                future.cancel(true);
                throw new IllegalStateException("LLM request timed out", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                throw new IllegalStateException("LLM request interrupted", exception);
            } catch (ExecutionException exception) {
                throw unwrapExecutionException(exception);
            }
        }
    }

    private RuntimeException unwrapExecutionException(final ExecutionException exception) {
        final Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("LLM request failed", cause);
    }

    private String extractContent(final ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("LLM response is empty");
        }

        final String content = response.getResult().getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM response content is blank");
        }
        return content.trim();
    }

    private String extractChunk(final ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }

        final String content = response.getResult().getOutput().getText();
        if (content == null) {
            return "";
        }
        return content;
    }

    private void logLlmStarted(final String mode, final long timeoutMs) {
        MDC.put("event", "llm_call_started");
        MDC.put("outcome", "started");
        try {
            log.info("event=llm_call_started mode={} timeout_ms={}", mode, timeoutMs);
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
    }

    private void logLlmCompleted(final String mode, final Instant startedAt) {
        final Duration elapsed = Duration.between(startedAt, Instant.now());
        MDC.put("event", "llm_call_completed");
        MDC.put("outcome", "success");
        try {
            log.info(
                    "event=llm_call_completed mode={} timeout_ms={} elapsed_ms={}",
                    mode,
                    resolveTimeoutMs(mode),
                    elapsed.toMillis()
            );
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
        chatMetrics.recordLlmCall(mode, "success", elapsed);
    }

    private void logLlmFailed(final String mode, final Instant startedAt, final Throwable exception) {
        final Duration elapsed = Duration.between(startedAt, Instant.now());
        final String outcome = exception instanceof CancellationException ? "cancelled" : "failure";
        MDC.put("event", "llm_call_failed");
        MDC.put("outcome", outcome);
        try {
            log.warn(
                    "event=llm_call_failed mode={} timeout_ms={} elapsed_ms={} exception_type={}",
                    mode,
                    resolveTimeoutMs(mode),
                    elapsed.toMillis(),
                    exception.getClass().getSimpleName(),
                    exception
            );
        } finally {
            MDC.remove("outcome");
            MDC.remove("event");
        }
        chatMetrics.recordLlmCall(mode, outcome, elapsed);
    }

    private long resolveTimeoutMs(final String mode) {
        if ("stream".equals(mode)) {
            return aiProperties.streamingTimeout().toMillis();
        }
        return aiProperties.llmTimeout().toMillis();
    }
}
