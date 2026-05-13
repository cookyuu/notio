package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.channel.domain.ChannelStatus;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.NotificationChannel;

import java.time.Instant;

public record ChannelResponse(
    Long id,
    @JsonProperty("channel_type") ChannelType channelType,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("target_identifier") String targetIdentifier,
    @JsonProperty("key_preview") String keyPreview,
    ChannelStatus status,
    @JsonProperty("error_count") int errorCount,
    @JsonProperty("last_error") String lastError,
    @JsonProperty("last_delivered_at") Instant lastDeliveredAt,
    @JsonProperty("created_at") Instant createdAt
) {
    public static ChannelResponse from(NotificationChannel channel, String keyPreview) {
        return new ChannelResponse(
            channel.getId(),
            channel.getChannelType(),
            channel.getDisplayName(),
            channel.getTargetIdentifier(),
            keyPreview,
            channel.getStatus(),
            channel.getErrorCount(),
            channel.getLastError(),
            channel.getLastDeliveredAt(),
            channel.getCreatedAt()
        );
    }
}
