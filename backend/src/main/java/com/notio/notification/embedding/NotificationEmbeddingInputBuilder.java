package com.notio.notification.embedding;

import com.notio.notification.domain.Notification;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmbeddingInputBuilder {

    private static final int MAX_METADATA_LENGTH = 500;

    public NotificationEmbeddingInput build(final Notification notification) {
        final String content = """
                source: %s
                priority: %s
                title: %s
                body: %s
                metadata: %s
                """.formatted(
                notification.getSource(),
                notification.getPriority(),
                normalize(notification.getTitle()),
                normalize(notification.getBody()),
                truncateMetadata(notification.getMetadata())
        ).trim();

        return new NotificationEmbeddingInput(content, sha256(content));
    }

    private String normalize(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.strip();
    }

    private String truncateMetadata(final String metadata) {
        final String normalized = normalize(metadata);
        if (normalized.length() <= MAX_METADATA_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_METADATA_LENGTH);
    }

    private String sha256(final String content) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
