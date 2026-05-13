package com.notio.channel.provider;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownFormatter {

    private static final String SPECIAL_CHARS = "_*[]()~`>#+\\-=|{}.!";

    public String escape(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (char c : text.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public String format(ChannelMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(escape(message.title())).append("*\n\n");
        sb.append(escape(message.body())).append("\n\n");
        sb.append("📌 *Source:* ").append(escape(message.source().name()));
        sb.append(" \\| *Priority:* ").append(escape(message.priority().name()));
        if (message.externalUrl() != null && !message.externalUrl().isBlank()) {
            sb.append("\n[자세히 보기](").append(message.externalUrl()).append(")");
        }
        return sb.toString();
    }
}
