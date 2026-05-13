package com.notio.channel.provider;

public record ChannelDeliveryResult(
    boolean success,
    String externalMessageId,
    String errorMessage,
    boolean retryable
) {
    public static ChannelDeliveryResult success(String messageId) {
        return new ChannelDeliveryResult(true, messageId, null, false);
    }

    public static ChannelDeliveryResult failure(String error, boolean retryable) {
        return new ChannelDeliveryResult(false, null, error, retryable);
    }
}
