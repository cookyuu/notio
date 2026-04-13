package com.notio.chat.dto;

import java.time.OffsetDateTime;

public record ChatMessageResponse(
        long id,
        String role,
        String content,
        OffsetDateTime createdAt
) {
}

