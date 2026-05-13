package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.ChannelStatus;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.metrics.ChannelDeliveryMetrics;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.provider.NotificationChannelProvider;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.channel.repository.NotificationChannelRepository;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChannelDeliverySchedulerTest {

    @Mock private ChannelDeliveryLogRepository deliveryLogRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationChannelRepository channelRepository;
    @Mock private ChannelProviderRegistry providerRegistry;
    @Mock private ChannelRouter channelRouter;
    @Mock private ChannelDeliveryMetrics metrics;

    private ChannelDeliveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ChannelDeliveryScheduler(
            deliveryLogRepository, notificationRepository, channelRepository,
            providerRegistry, channelRouter, metrics
        );
    }

    @Test
    void processRetriesTransitionsToDeadAfterThreeAttempts() {
        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(1L).channelId(1L)
            .status(DeliveryStatus.RETRY).attemptCount(2).build();

        when(deliveryLogRepository.findTop50ByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.RETRY), any(Instant.class))).thenReturn(List.of(log));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(activeChannel()));

        scheduler.processRetries();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
        assertThat(log.getAttemptCount()).isEqualTo(3);
        verify(deliveryLogRepository).save(log);
        verify(metrics).recordDead(ChannelType.SLACK.name());
    }

    @Test
    void processRetriesTransitionsToSuccessOnSuccessfulDelivery() {
        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(1L).channelId(1L)
            .status(DeliveryStatus.RETRY).attemptCount(0).build();
        NotificationChannel channel = activeChannel();
        Notification notification = notification();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findTop50ByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.RETRY), any(Instant.class))).thenReturn(List.of(log));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.success("msg-id"));

        scheduler.processRetries();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log.getExternalMessageId()).isEqualTo("msg-id");
        assertThat(log.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void processRetriesTransitionsToRetryOnRetryableFailure() {
        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(1L).channelId(1L)
            .status(DeliveryStatus.RETRY).attemptCount(0).build();
        NotificationChannel channel = activeChannel();
        Notification notification = notification();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findTop50ByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.RETRY), any(Instant.class))).thenReturn(List.of(log));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("rate limit", true));
        when(channelRouter.computeNextRetry(1)).thenReturn(Instant.now().plusSeconds(300));

        scheduler.processRetries();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.RETRY);
        assertThat(log.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void processRetriesTransitionsToDeadOnNonRetryableFailure() {
        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(1L).channelId(1L)
            .status(DeliveryStatus.RETRY).attemptCount(1).build();
        NotificationChannel channel = activeChannel();
        Notification notification = notification();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findTop50ByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.RETRY), any(Instant.class))).thenReturn(List.of(log));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("invalid token", false));

        scheduler.processRetries();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
    }

    @Test
    void processRetriesSkipsPausedChannel() {
        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(1L).channelId(1L)
            .status(DeliveryStatus.RETRY).attemptCount(0).build();
        NotificationChannel pausedChannel = NotificationChannel.builder()
            .id(1L).userId(1L).channelType(ChannelType.SLACK)
            .displayName("test").credentialEncrypted("enc")
            .status(ChannelStatus.PAUSED).build();

        when(deliveryLogRepository.findTop50ByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.RETRY), any(Instant.class))).thenReturn(List.of(log));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(pausedChannel));

        scheduler.processRetries();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
        verify(providerRegistry, never()).get(any());
    }

    @Test
    void processRetriesDoesNothingWhenNoRetryLogsExist() {
        when(deliveryLogRepository.findTop50ByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.RETRY), any(Instant.class))).thenReturn(List.of());

        scheduler.processRetries();

        verify(deliveryLogRepository, never()).save(any());
    }

    private NotificationChannel activeChannel() {
        return NotificationChannel.builder()
            .id(1L).userId(1L).channelType(ChannelType.SLACK)
            .displayName("test").credentialEncrypted("enc")
            .status(ChannelStatus.ACTIVE).build();
    }

    private Notification notification() {
        return Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.GITHUB)
            .title("Test").body("Body").priority(NotificationPriority.HIGH).build();
    }
}
