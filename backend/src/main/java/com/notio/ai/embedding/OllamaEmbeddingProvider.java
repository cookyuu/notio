package com.notio.ai.embedding;

import com.notio.common.exception.AiExceptionTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;
    private final AiExceptionTranslator exceptionTranslator;

    @Override
    public float[] embed(final String input) {
        try {
            return embeddingModel.embed(input);
        } catch (RuntimeException exception) {
            throw exceptionTranslator.embeddingFailed(exception);
        }
    }
}
