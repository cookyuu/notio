package com.notio.webhook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notio.webhook")
public record WebhookProperties(
        Slack slack,
        Github github
) {

    public record Slack(String signingSecret) {
    }

    public record Github(String webhookSecret) {
    }
}
