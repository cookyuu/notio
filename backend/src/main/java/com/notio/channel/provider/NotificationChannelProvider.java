package com.notio.channel.provider;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.NotificationChannel;

public interface NotificationChannelProvider {

    ChannelType supports();

    ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message);

    ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier);
}
