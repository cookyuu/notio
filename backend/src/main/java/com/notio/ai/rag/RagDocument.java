package com.notio.ai.rag;

import java.time.Instant;

public record RagDocument(
        Long notificationId,
        String source,
        String title,
        String bodySummary,
        String priority,
        Instant createdAt,
        double similarityScore
) {
}
