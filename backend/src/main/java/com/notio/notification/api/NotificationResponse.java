package com.notio.notification.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import lombok.Builder;

import java.util.Map;

@Builder
public record NotificationResponse(
    Long id,
    String source,
    String title,
    String body,
    String priority,

    @JsonProperty("is_read")
    boolean isRead,

    @JsonProperty("created_at")
    String createdAt,

    @JsonProperty("updated_at")
    String updatedAt,

    @JsonProperty("external_id")
    String externalId,

    @JsonProperty("external_url")
    String externalUrl,

    Object metadata
) {
    public static NotificationResponse from(Notification notification, NotificationService service) {
        Map<String, Object> metadata = service.parseMetadataFromJson(notification.getMetadata());

        return NotificationResponse.builder()
            .id(notification.getId())
            .source(notification.getSource().name())
            .title(notification.getTitle())
            .body(notification.getBody())
            .priority(notification.getPriority().name())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedAt().toString())
            .updatedAt(notification.getUpdatedAt().toString())
            .externalId(notification.getExternalId())
            .externalUrl(notification.getExternalUrl())
            .metadata(metadata)
            .build();
    }
}
