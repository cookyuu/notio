package com.notio.connection.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator {

    public static final String API_KEY_PREFIX = "ntio_wh";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PREFIX_BYTES = 9;
    private static final int SECRET_BYTES = 32;

    public GeneratedApiKey generate() {
        final String prefix = randomToken(PREFIX_BYTES);
        final String secret = randomToken(SECRET_BYTES);
        final String value = API_KEY_PREFIX + "_" + prefix + "_" + secret;
        return new GeneratedApiKey(value, prefix, API_KEY_PREFIX + "_" + prefix + "_..." + secret.substring(secret.length() - 4));
    }

    private String randomToken(final int byteLength) {
        final byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
