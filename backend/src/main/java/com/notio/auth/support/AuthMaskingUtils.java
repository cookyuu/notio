package com.notio.auth.support;

import java.util.List;
import java.util.Objects;

public final class AuthMaskingUtils {

    private static final String MASK = "***";

    private AuthMaskingUtils() {
    }

    public static String maskEmail(final String email) {
        if (email == null || email.isBlank()) {
            return MASK;
        }

        final int atIndex = email.indexOf('@');
        if (atIndex <= 1 || atIndex == email.length() - 1) {
            return MASK;
        }

        final String localPart = email.substring(0, atIndex);
        final String domain = email.substring(atIndex + 1);
        return localPart.charAt(0) + MASK + "@" + domain;
    }

    public static String maskSecrets(final String value, final List<String> secrets) {
        if (value == null || value.isBlank() || secrets == null || secrets.isEmpty()) {
            return value;
        }

        String masked = value;
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                masked = masked.replace(secret, MASK);
            }
        }
        return masked;
    }

    public static List<String> normalizeSecrets(final List<String> secrets) {
        if (secrets == null || secrets.isEmpty()) {
            return List.of();
        }
        return secrets.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(secret -> !secret.isBlank())
                .distinct()
                .toList();
    }
}
