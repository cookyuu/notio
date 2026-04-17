package com.notio.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingAuthMailSenderTest {

    @Test
    void sendLogsMaskedRecipientAndSensitiveBodyContent() {
        final LoggingAuthMailSender sender = new LoggingAuthMailSender();
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingAuthMailSender.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            sender.send(new AuthMailMessage(
                    "user@example.com",
                    "Reset password",
                    "reset link token=secret-token",
                    List.of("secret-token")
            ));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("u***@example.com");
        assertThat(event.getFormattedMessage()).contains("***");
        assertThat(event.getFormattedMessage()).doesNotContain("user@example.com");
        assertThat(event.getFormattedMessage()).doesNotContain("secret-token");
    }
}
