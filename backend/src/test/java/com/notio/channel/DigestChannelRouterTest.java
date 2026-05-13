package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.DeliveryMode;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.domain.RoutingCondition;
import com.notio.channel.domain.RoutingRule;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigestChannelRouterTest {

    @Mock
    private ChannelDeliveryLogRepository deliveryLogRepository;

    private DigestChannelRouter digestChannelRouter;

    @BeforeEach
    void setUp() {
        digestChannelRouter = new DigestChannelRouter(deliveryLogRepository);
    }

    @Test
    void queueCreatesNewWindowWhenNoPendingWindowExists() {
        Notification notification = notification();
        NotificationChannel channel = channel();
        RoutingRule rule = rule(30);

        when(deliveryLogRepository.existsByChannelIdAndStatus(1L, DeliveryStatus.DIGEST_PENDING)).thenReturn(false);
        Instant before = Instant.now();

        digestChannelRouter.queue(notification, channel, rule);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());

        ChannelDeliveryLog saved = logCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.DIGEST_PENDING);
        assertThat(saved.getNotificationId()).isEqualTo(1L);
        assertThat(saved.getChannelId()).isEqualTo(1L);
        assertThat(saved.getNextRetryAt()).isAfter(before.plus(29, ChronoUnit.MINUTES));
        assertThat(saved.getNextRetryAt()).isBefore(before.plus(31, ChronoUnit.MINUTES));
    }

    @Test
    void queueReusesExistingWindowEndWhenPendingWindowExists() {
        Notification notification = notification();
        NotificationChannel channel = channel();
        RoutingRule rule = rule(60);
        Instant existingWindowEnd = Instant.now().plus(45, ChronoUnit.MINUTES);

        when(deliveryLogRepository.existsByChannelIdAndStatus(1L, DeliveryStatus.DIGEST_PENDING)).thenReturn(true);
        when(deliveryLogRepository.findMinNextRetryAtByChannelIdAndStatus(1L, DeliveryStatus.DIGEST_PENDING))
            .thenReturn(Optional.of(existingWindowEnd));

        digestChannelRouter.queue(notification, channel, rule);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getNextRetryAt()).isEqualTo(existingWindowEnd);
    }

    @Test
    void queueUsesRuleWindowWhenExistingWindowHasNoTimestamp() {
        Notification notification = notification();
        NotificationChannel channel = channel();
        RoutingRule rule = rule(15);

        when(deliveryLogRepository.existsByChannelIdAndStatus(1L, DeliveryStatus.DIGEST_PENDING)).thenReturn(true);
        when(deliveryLogRepository.findMinNextRetryAtByChannelIdAndStatus(1L, DeliveryStatus.DIGEST_PENDING))
            .thenReturn(Optional.empty());
        Instant before = Instant.now();

        digestChannelRouter.queue(notification, channel, rule);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getNextRetryAt())
            .isAfter(before.plus(14, ChronoUnit.MINUTES))
            .isBefore(before.plus(16, ChronoUnit.MINUTES));
    }

    @Test
    void queueUsesDefaultIntervalOf30MinutesWhenDigestIntervalMinIsNull() {
        Notification notification = notification();
        NotificationChannel channel = channel();
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("rule").conditions(RoutingCondition.empty())
            .channelIds(List.of(1L)).deliveryMode(DeliveryMode.DIGEST)
            .digestIntervalMin(null).build();

        when(deliveryLogRepository.existsByChannelIdAndStatus(1L, DeliveryStatus.DIGEST_PENDING)).thenReturn(false);
        Instant before = Instant.now();

        digestChannelRouter.queue(notification, channel, rule);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getNextRetryAt())
            .isAfter(before.plus(29, ChronoUnit.MINUTES))
            .isBefore(before.plus(31, ChronoUnit.MINUTES));
    }

    private Notification notification() {
        return Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.GITHUB)
            .title("Test").body("Body").priority(NotificationPriority.HIGH).build();
    }

    private NotificationChannel channel() {
        return NotificationChannel.builder()
            .id(1L).userId(1L).channelType(ChannelType.SLACK)
            .displayName("test").credentialEncrypted("enc").build();
    }

    private RoutingRule rule(int intervalMin) {
        return RoutingRule.builder()
            .userId(1L).ruleName("rule").conditions(RoutingCondition.empty())
            .channelIds(List.of(1L)).deliveryMode(DeliveryMode.DIGEST)
            .digestIntervalMin(intervalMin).build();
    }
}
