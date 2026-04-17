package com.notio.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.notification.domain.Notification;
import lombok.Builder;

@Builder
public record NotificationSummaryResponse(
    Long id,
    String source,
    String title,
    String priority,

    @JsonProperty("is_read")
    boolean isRead,

    @JsonProperty("created_at")
    String createdAt,

    @JsonProperty("body_preview")
    String bodyPreview
) {

    private static final int BODY_PREVIEW_MAX_LENGTH = 120;

    public static NotificationSummaryResponse from(Notification notification) {
        return NotificationSummaryResponse.builder()
            .id(notification.getId())
            .source(notification.getSource().name())
            .title(notification.getTitle())
            .priority(notification.getPriority().name())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedAt().toString())
            .bodyPreview(createBodyPreview(notification.getBody()))
            .build();
    }

    static String createBodyPreview(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        String normalized = body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= BODY_PREVIEW_MAX_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, BODY_PREVIEW_MAX_LENGTH - 3) + "...";
    }
}
