package com.notio.common.config.properties;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notio.ai")
public record NotioAiProperties(
        @DefaultValue("ollama") String provider,
        Duration llmTimeout,
        Duration embeddingTimeout,
        Duration streamingTimeout,
        @DefaultValue({"CLAUDE", "CODEX"}) List<String> summarizeSources
) {
}
