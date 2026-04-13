package com.notio.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notio.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

    public CorsProperties {
        allowedOriginPatterns = allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()
                ? List.of("http://localhost:[*]", "http://127.0.0.1:[*]")
                : List.copyOf(allowedOriginPatterns);
    }
}
