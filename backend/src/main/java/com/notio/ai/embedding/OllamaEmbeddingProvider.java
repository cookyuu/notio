package com.notio.ai.embedding;

import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;
    private final AiExceptionTranslator exceptionTranslator;
    private final NotioAiProperties aiProperties;

    @Override
    public float[] embed(final String input) {
        try {
            return embedWithTimeout(input);
        } catch (RuntimeException exception) {
            throw exceptionTranslator.embeddingFailed(exception);
        }
    }

    private float[] embedWithTimeout(final String input) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            final Future<float[]> future = executorService.submit(() -> embeddingModel.embed(input));
            try {
                return future.get(aiProperties.embeddingTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                future.cancel(true);
                throw new IllegalStateException("Embedding request timed out", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                throw new IllegalStateException("Embedding request interrupted", exception);
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
        return new IllegalStateException("Embedding request failed", cause);
    }
}
