package com.notio.common.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notio.ai")
public record NotioAiProperties(
        Duration llmTimeout,
        Duration embeddingTimeout,
        Duration streamingTimeout
) {
}
