package com.notio.channel.provider;

public record ChannelValidationResult(boolean isValid, String errorMessage) {

    public static ChannelValidationResult valid() {
        return new ChannelValidationResult(true, null);
    }

    public static ChannelValidationResult invalid(String msg) {
        return new ChannelValidationResult(false, msg);
    }
}
