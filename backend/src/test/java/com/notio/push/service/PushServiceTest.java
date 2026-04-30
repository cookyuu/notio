package com.notio.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.repository.NotificationRepository;
import com.notio.push.repository.DeviceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PushServiceTest {

    @Test
    void sendPushLogsDispatchFailureAndRecordsMetricWhenFirebaseIsNotInitialized() {
        final NotificationRepository notificationRepository = mock(NotificationRepository.class);
        final DeviceRepository deviceRepository = mock(DeviceRepository.class);
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        final PushService pushService = new PushService(
                notificationRepository,
                deviceRepository,
                new NotificationFlowMetrics(new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy()))
        );

        final Logger logger = (Logger) LoggerFactory.getLogger(PushService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            pushService.sendPush(99L, 1L);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
                .contains("event=push_dispatch_failed")
                .contains("notification_id=99")
                .contains("reason=firebase_not_initialized");
        assertThat(meterRegistry.get("notio_push_send_total")
                .tag("outcome", "failure")
                .counter()
                .count()).isEqualTo(1.0d);
        verifyNoInteractions(notificationRepository, deviceRepository);
    }
}
