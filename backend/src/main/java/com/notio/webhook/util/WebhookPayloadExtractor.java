package com.notio.webhook.util;

import com.notio.notification.domain.NotificationPriority;
import java.util.Map;

public final class WebhookPayloadExtractor {

    private WebhookPayloadExtractor() {
    }

    public static String stringValue(final Map<String, Object> payload, final String... keys) {
        for (String key : keys) {
            final Object value = nestedValue(payload, key);
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return stringValue;
            }
        }
        return null;
    }

    public static Long longValue(final Map<String, Object> payload, final String... keys) {
        for (String key : keys) {
            final Object value = nestedValue(payload, key);
            if (value instanceof Number numberValue) {
                return numberValue.longValue();
            }
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                try {
                    return Long.valueOf(stringValue);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static NotificationPriority priorityValue(final Map<String, Object> payload, final String... keys) {
        final String value = stringValue(payload, keys);
        return value == null ? NotificationPriority.MEDIUM : NotificationPriority.from(value);
    }

    @SuppressWarnings("unchecked")
    private static Object nestedValue(final Map<String, Object> payload, final String path) {
        Object current = payload;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = ((Map<String, Object>) currentMap).get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
