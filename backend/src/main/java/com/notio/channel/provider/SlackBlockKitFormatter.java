package com.notio.channel.provider;

import com.notio.notification.domain.NotificationPriority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SlackBlockKitFormatter {

    private static final Map<NotificationPriority, String> COLORS = Map.of(
        NotificationPriority.URGENT, "#FF0000",
        NotificationPriority.HIGH,   "#FF8C00",
        NotificationPriority.MEDIUM, "#4A90E2",
        NotificationPriority.LOW,    "#9B9B9B"
    );

    public Map<String, Object> format(String channel, ChannelMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", channel);

        List<Map<String, Object>> blocks = new ArrayList<>();

        blocks.add(section("*" + escapeMarkdown(message.title()) + "*"));
        blocks.add(section(truncate(message.body(), 3000)));

        String contextText = buildContextText(message);
        blocks.add(context(contextText));

        List<Map<String, Object>> attachments = new ArrayList<>();
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", COLORS.getOrDefault(message.priority(), "#9B9B9B"));
        attachment.put("blocks", blocks);
        attachments.add(attachment);

        payload.put("attachments", attachments);
        return payload;
    }

    private Map<String, Object> section(String text) {
        Map<String, Object> textObj = new LinkedHashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", text);

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "section");
        block.put("text", textObj);
        return block;
    }

    private Map<String, Object> context(String text) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "mrkdwn");
        element.put("text", text);

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "context");
        block.put("elements", List.of(element));
        return block;
    }

    private String buildContextText(ChannelMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Source:* ").append(message.source());
        sb.append(" | *Priority:* ").append(message.priority());
        if (message.externalUrl() != null && !message.externalUrl().isBlank()) {
            sb.append(" | <").append(message.externalUrl()).append("|자세히 보기>");
        }
        return sb.toString();
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
    }
}
