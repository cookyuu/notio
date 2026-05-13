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
public class SlackChannelProvider implements NotificationChannelProvider {

    private static final String CHAT_POST_URL = "https://slack.com/api/chat.postMessage";
    private static final String AUTH_TEST_URL  = "https://slack.com/api/auth.test";

    private final RestClient restClient;
    private final CredentialEncryptionService encryptionService;
    private final SlackBlockKitFormatter formatter;

    @Override
    public ChannelType supports() {
        return ChannelType.SLACK;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelDeliveryResult deliver(NotificationChannel channel, ChannelMessage message) {
        String token = encryptionService.decrypt(channel.getCredentialEncrypted());
        Map<String, Object> payload = formatter.format(channel.getTargetIdentifier(), message);
        try {
            Map<String, Object> response = restClient.post()
                .uri(CHAT_POST_URL)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                return ChannelDeliveryResult.failure("Empty response from Slack", true);
            }
            Boolean ok = (Boolean) response.get("ok");
            if (Boolean.TRUE.equals(ok)) {
                return ChannelDeliveryResult.success((String) response.get("ts"));
            }
            String error = (String) response.get("error");
            boolean retryable = "ratelimited".equals(error);
            return ChannelDeliveryResult.failure(error, retryable);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return ChannelDeliveryResult.failure(e.getMessage(), true);
            }
            return ChannelDeliveryResult.failure(e.getMessage(), false);
        } catch (HttpServerErrorException e) {
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        } catch (Exception e) {
            log.warn("Slack delivery unexpected error: {}", e.getMessage());
            return ChannelDeliveryResult.failure(e.getMessage(), true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelValidationResult validate(String credentialPlaintext, String targetIdentifier) {
        try {
            Map<String, Object> response = restClient.post()
                .uri(AUTH_TEST_URL)
                .header("Authorization", "Bearer " + credentialPlaintext)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                return ChannelValidationResult.invalid("Empty response from Slack auth.test");
            }
            if (Boolean.TRUE.equals(response.get("ok"))) {
                return ChannelValidationResult.valid();
            }
            return ChannelValidationResult.invalid("Slack auth.test failed: " + response.get("error"));
        } catch (Exception e) {
            return ChannelValidationResult.invalid("Slack validation error: " + e.getMessage());
        }
    }
}
