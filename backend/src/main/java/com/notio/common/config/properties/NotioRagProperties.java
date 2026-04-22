package com.notio.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notio.rag")
public record NotioRagProperties(
        int topK,
        int embeddingDimension
) {
}
