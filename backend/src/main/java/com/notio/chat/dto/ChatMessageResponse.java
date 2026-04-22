package com.notio.chat.dto;

import com.notio.chat.domain.ChatMessage;
import java.time.OffsetDateTime;

public record ChatMessageResponse(
        long id,
        String role,
        String content,
        OffsetDateTime createdAt
) {
    public static ChatMessageResponse from(final ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole().name().toLowerCase(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}

