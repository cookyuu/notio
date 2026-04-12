package com.notio.notification.domain;

public enum NotificationPriority {
    URGENT,
    HIGH,
    MEDIUM,
    LOW;

    public static NotificationPriority from(final String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }

        return NotificationPriority.valueOf(value.trim().toUpperCase());
    }
}
