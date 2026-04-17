package com.notio.auth.mail;

import com.notio.auth.support.AuthMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "notio.auth.mail", name = "delivery-mode", havingValue = "LOG")
public class LoggingAuthMailSender implements AuthMailSender {

    @Override
    public void send(final AuthMailMessage message) {
        log.info(
                "Auth mail queued: recipient={}, subject={}, body={}",
                AuthMaskingUtils.maskEmail(message.recipientEmail()),
                message.subject(),
                AuthMaskingUtils.maskSecrets(message.body(), message.sensitiveValues())
        );
    }
}
