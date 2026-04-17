package com.notio.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class NoopAuthMailSenderTest {

    @Test
    void sendLogsConfigurationWarningWithoutExposingRawRecipient() {
        final NoopAuthMailSender sender = new NoopAuthMailSender();
        final Logger logger = (Logger) LoggerFactory.getLogger(NoopAuthMailSender.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            sender.send(new AuthMailMessage(
                    "user@example.com",
                    "Find ID",
                    "body",
                    List.of()
            ));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("u***@example.com");
        assertThat(event.getFormattedMessage()).doesNotContain("user@example.com");
        assertThat(event.getFormattedMessage()).contains("Find ID");
    }
}
