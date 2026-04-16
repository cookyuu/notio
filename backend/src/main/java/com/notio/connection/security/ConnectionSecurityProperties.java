package com.notio.connection.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notio.connection.security")
public record ConnectionSecurityProperties(
    String webhookKeyPepper,
    String credentialEncryptionKey
) {
}
