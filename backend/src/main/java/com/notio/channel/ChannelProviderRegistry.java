package com.notio.channel;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.provider.NotificationChannelProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ChannelProviderRegistry {

    private final Map<ChannelType, NotificationChannelProvider> registry;

    public ChannelProviderRegistry(List<NotificationChannelProvider> providers) {
        this.registry = providers.stream()
            .collect(Collectors.toMap(NotificationChannelProvider::supports, p -> p));
    }

    public NotificationChannelProvider get(ChannelType type) {
        return Optional.ofNullable(registry.get(type))
            .orElseThrow(() -> new IllegalArgumentException("No provider registered for channel type: " + type));
    }

    public boolean supports(ChannelType type) {
        return registry.containsKey(type);
    }
}
