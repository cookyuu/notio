package com.notio.common.ratelimit;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notio.rate-limit")
public class RateLimitProperties {

    private List<String> failOpenProfiles = List.of("local", "dev", "test");
    private List<String> failClosedPaths = List.of("/api/v1/webhook/**");
    private int webhookBodyMaxBytes = 65_536;
    private int jsonBodyMaxBytes = 1_048_576;
    private int chatMaxChars = 4_000;

    public List<String> getFailOpenProfiles() {
        return failOpenProfiles;
    }

    public void setFailOpenProfiles(final List<String> failOpenProfiles) {
        this.failOpenProfiles = failOpenProfiles;
    }

    public List<String> getFailClosedPaths() {
        return failClosedPaths;
    }

    public void setFailClosedPaths(final List<String> failClosedPaths) {
        this.failClosedPaths = failClosedPaths;
    }

    public int getWebhookBodyMaxBytes() {
        return webhookBodyMaxBytes;
    }

    public void setWebhookBodyMaxBytes(final int webhookBodyMaxBytes) {
        this.webhookBodyMaxBytes = webhookBodyMaxBytes;
    }

    public int getJsonBodyMaxBytes() {
        return jsonBodyMaxBytes;
    }

    public void setJsonBodyMaxBytes(final int jsonBodyMaxBytes) {
        this.jsonBodyMaxBytes = jsonBodyMaxBytes;
    }

    public int getChatMaxChars() {
        return chatMaxChars;
    }

    public void setChatMaxChars(final int chatMaxChars) {
        this.chatMaxChars = chatMaxChars;
    }
}
