package com.notio.channel.provider;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.NotificationChannel;
import com.notio.connection.security.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramChannelProvider implements NotificationChannelProvider {

    private static final String API_BASE = "https://api.telegram.org/bot";

    private final RestClient restClient;
    private final CredentialEncryptionService encryptionService;
    private final TelegramMarkdownFormatter formatter;

    @Override
    public ChannelType supports() {
        return ChannelType.TELEGRAM;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message) {
        String token = encryptionService.decrypt(channel.getCredentialEncrypted());
        String chatId = channel.getTargetIdentifier();
        String text = formatter.format(message);
        String url = API_BASE + token + "/sendMessage";

        Map<String, Object> payload = Map.of(
            "chat_id", chatId,
            "text", text,
            "parse_mode", "MarkdownV2"
        );

        try {
            Map<String, Object> response = restClient.post()
                .uri(url)
                .body(payload)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                return ChannelDeliveryResult.failure("Empty response from Telegram", true);
            }
            Boolean ok = (Boolean) response.get("ok");
            if (Boolean.TRUE.equals(ok)) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                String messageId = result != null ? String.valueOf(result.get("message_id")) : null;
                return ChannelDeliveryResult.success(messageId);
            }
            return ChannelDeliveryResult.failure("Telegram sendMessage failed", false);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return ChannelDeliveryResult.failure(e.getMessage(), true);
            }
            return ChannelDeliveryResult.failure(e.getMessage(), false);
        } catch (HttpServerErrorException e) {
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        } catch (Exception e) {
            log.warn("Telegram delivery unexpected error: {}", e.getMessage());
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier) {
        String url = API_BASE + credentialPlaintext + "/getMe";
        try {
            Map<String, Object> response = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                return ChannelValidationResult.invalid("Empty response from Telegram getMe");
            }
            if (Boolean.TRUE.equals(response.get("ok"))) {
                return ChannelValidationResult.valid();
            }
            return ChannelValidationResult.invalid("Telegram getMe failed: " + response.get("description"));
        } catch (Exception e) {
            return ChannelValidationResult.invalid("Telegram validation error: " + e.getMessage());
        }
    }
}
