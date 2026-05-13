package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.DeliveryStatus;

import java.time.Instant;

public record DeliveryFeedItem(
    @JsonProperty("delivery_log_id") Long deliveryLogId,
    @JsonProperty("notification_id") Long notificationId,
    @JsonProperty("notification_title") String notificationTitle,
    @JsonProperty("channel_id") Long channelId,
    @JsonProperty("channel_type") ChannelType channelType,
    @JsonProperty("channel_display_name") String channelDisplayName,
    @JsonProperty("delivered_content") String deliveredContent,
    @JsonProperty("delivered_at") Instant deliveredAt,
    DeliveryStatus status,
    @JsonProperty("external_message_id") String externalMessageId
) {}
