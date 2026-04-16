package com.notio.connection.security;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class CredentialEncryptionService {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConnectionSecurityProperties properties;
    private final Environment environment;

    public CredentialEncryptionService(
        final ConnectionSecurityProperties properties,
        final Environment environment
    ) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validateKeyPolicy() {
        if (hasEncryptionKey() || isLocalTestOrDefaultProfile()) {
            return;
        }
        throw new IllegalStateException("NOTIO_CREDENTIAL_ENCRYPTION_KEY is required outside local/test profiles.");
    }

    public String encrypt(final String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            final byte[] nonce = new byte[NONCE_BYTES];
            SECURE_RANDOM.nextBytes(nonce);
            final Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            final byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            final ByteBuffer buffer = ByteBuffer.allocate(nonce.length + encrypted.length);
            buffer.put(nonce);
            buffer.put(encrypted);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(final String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            final byte[] payload = Base64.getUrlDecoder().decode(encryptedText);
            final byte[] nonce = Arrays.copyOfRange(payload, 0, NONCE_BYTES);
            final byte[] encrypted = Arrays.copyOfRange(payload, NONCE_BYTES, payload.length);
            final Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private SecretKeySpec keySpec() {
        final String encodedKey = properties.credentialEncryptionKey();
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR, "Credential encryption key is not configured.");
        }
        final byte[] key = Base64.getUrlDecoder().decode(encodedKey);
        if (key.length != 32) {
            throw new NotioException(ErrorCode.INTERNAL_SERVER_ERROR, "Credential encryption key must decode to 32 bytes.");
        }
        return new SecretKeySpec(key, "AES");
    }

    private boolean hasEncryptionKey() {
        return properties.credentialEncryptionKey() != null && !properties.credentialEncryptionKey().isBlank();
    }

    private boolean isLocalTestOrDefaultProfile() {
        final String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 || Arrays.stream(activeProfiles)
            .anyMatch(profile -> profile.equals("local") || profile.equals("test"));
    }
}
