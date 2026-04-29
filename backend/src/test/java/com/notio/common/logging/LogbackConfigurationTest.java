package com.notio.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LogbackConfigurationTest {

    @Test
    void logbackSpringConfigurationContainsProfileSpecificFormatsAndCoreFields() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertThat(inputStream).isNotNull();

            final String config = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(config).contains("<springProfile name=\"prod\">");
            assertThat(config).contains("<springProfile name=\"!prod\">");
            assertThat(config).contains("LoggingEventCompositeJsonEncoder");
            assertThat(config).contains("correlation_id=%X{correlation_id:-}");
            assertThat(config).contains("\"event\":\"%mdc{event:-}\"");
            assertThat(config).contains("\"outcome\":\"%mdc{outcome:-}\"");
            assertThat(config).contains("\"route\":\"%mdc{route:-}\"");
            assertThat(config).contains("\"http_method\":\"%mdc{http_method:-}\"");
            assertThat(config).contains("<fieldName>stack_trace</fieldName>");
        }
    }
}
