package com.notio.auth.mail;

import com.notio.auth.support.AuthMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!local & !dev & !test")
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
