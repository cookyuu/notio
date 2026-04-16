package com.notio.connection.security;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class ConnectionCredentialHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ConnectionSecurityProperties properties;

    public ConnectionCredentialHasher(final ConnectionSecurityProperties properties) {
        this.properties = properties;
    }

    public String hash(final String value) {
        final String pepper = properties.webhookKeyPepper();
        if (pepper == null || pepper.isBlank()) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR, "Webhook key pepper is not configured.");
        }

        try {
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean matches(final String candidate, final String expectedHash) {
        if (candidate == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(candidate).getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
