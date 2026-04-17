package com.notio.auth.mail;

import com.notio.auth.support.AuthMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "notio.auth.mail", name = "delivery-mode", havingValue = "NOOP", matchIfMissing = true)
public class NoopAuthMailSender implements AuthMailSender {

    @Override
    public void send(final AuthMailMessage message) {
        log.warn(
                "Auth mail sender is not configured for this profile: recipient={}, subject={}",
                AuthMaskingUtils.maskEmail(message.recipientEmail()),
                message.subject()
        );
    }
}
