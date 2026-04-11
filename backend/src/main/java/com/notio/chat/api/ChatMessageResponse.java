package com.notio.chat.api;

import java.time.OffsetDateTime;

public record ChatMessageResponse(
        long id,
        String role,
        String content,
        OffsetDateTime createdAt
) {
}

