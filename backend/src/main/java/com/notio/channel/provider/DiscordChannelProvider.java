package com.notio.channel.provider;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.NotificationChannel;
import com.notio.connection.security.CredentialEncryptionService;
import com.notio.notification.domain.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordChannelProvider implements NotificationChannelProvider {

    private static final Map<NotificationPriority, Integer> COLORS = Map.of(
        NotificationPriority.URGENT, 16711680,
        NotificationPriority.HIGH,   16744448,
        NotificationPriority.MEDIUM, 4886754,
        NotificationPriority.LOW,    10197915
    );

    private final RestClient restClient;
    private final CredentialEncryptionService encryptionService;

    @Override
    public ChannelType supports() {
        return ChannelType.DISCORD;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message) {
        try {
            String webhookUrl = encryptionService.decrypt(channel.getCredentialEncrypted());

            Map<String, Object> embed = new LinkedHashMap<>();
            embed.put("title", message.title());
            embed.put("description", truncate(message.body(), 4096));
            embed.put("color", COLORS.getOrDefault(message.priority(), 10197915));

            List<Map<String, Object>> fields = List.of(
                field("Source", message.source().name(), true),
                field("Priority", message.priority().name(), true)
            );
            embed.put("fields", fields);

            if (message.externalUrl() != null && !message.externalUrl().isBlank()) {
                embed.put("url", message.externalUrl());
            }

            Map<String, Object> payload = Map.of("embeds", List.of(embed));

            Map<String, Object> response = restClient.post()
                .uri(webhookUrl + "?wait=true")
                .body(payload)
                .retrieve()
                .body(Map.class);

            String messageId = response != null ? String.valueOf(response.get("id")) : null;
            return ChannelDeliveryResult.success(messageId);

        } catch (HttpClientErrorException e) {
            log.warn("event=discord_delivery_failed channel_id={} status={}", channel.getId(), e.getStatusCode().value());
            boolean retryable = HttpStatus.resolve(e.getStatusCode().value()) == HttpStatus.TOO_MANY_REQUESTS;
            return ChannelDeliveryResult.failure(e.getMessage(), retryable);
        } catch (HttpServerErrorException e) {
            log.warn("event=discord_delivery_server_error channel_id={} status={}", channel.getId(), e.getStatusCode().value());
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        } catch (Exception e) {
            log.warn("event=discord_delivery_exception channel_id={}", channel.getId(), e);
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier) {
        try {
            Map<String, Object> response = restClient.get()
                .uri(credentialPlaintext)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("id")) {
                return ChannelValidationResult.valid();
            }
            return ChannelValidationResult.invalid("Discord webhook validation failed: unexpected response");
        } catch (Exception e) {
            return ChannelValidationResult.invalid("Discord validation error: " + e.getMessage());
        }
    }

    private Map<String, Object> field(String name, String value, boolean inline) {
        return Map.of("name", name, "value", value, "inline", inline);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
    }
}
