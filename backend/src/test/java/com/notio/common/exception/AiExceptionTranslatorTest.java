package com.notio.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiExceptionTranslatorTest {

    private final AiExceptionTranslator translator = new AiExceptionTranslator();

    @Test
    void embeddingFailedMapsMissingOllamaModelToLlmUnavailable() {
        final RuntimeException cause = new RuntimeException(
                "HTTP 404 - {\"error\":\"model \\\"nomic-embed-text\\\" not found, try pulling it first\"}"
        );

        final NotioException exception = translator.embeddingFailed(cause);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void embeddingFailedKeepsUnexpectedEmbeddingError() {
        final RuntimeException cause = new RuntimeException("invalid embedding dimension");

        final NotioException exception = translator.embeddingFailed(cause);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMBEDDING_FAILED);
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
