package com.notio.auth.mail;

import com.notio.auth.support.AuthMaskingUtils;
import java.util.List;

public record AuthMailMessage(
        String recipientEmail,
        String subject,
        String body,
        List<String> sensitiveValues
) {
    public AuthMailMessage {
        sensitiveValues = AuthMaskingUtils.normalizeSecrets(sensitiveValues);
    }
}
