package com.notio.auth.mail;

import com.notio.auth.support.AuthMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "dev", "test"})
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
