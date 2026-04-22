package com.notio.ai.embedding;

public interface EmbeddingProvider {

    float[] embed(String input);
}
