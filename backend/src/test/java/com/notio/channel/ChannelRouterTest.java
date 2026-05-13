package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.ChannelStatus;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.DeliveryMode;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.domain.RoutingCondition;
import com.notio.channel.domain.RoutingRule;
import com.notio.channel.metrics.ChannelDeliveryMetrics;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.provider.ChannelMessage;
import com.notio.channel.provider.NotificationChannelProvider;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.channel.repository.NotificationChannelRepository;
import com.notio.channel.repository.RoutingRuleRepository;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChannelRouterTest {

    @Mock private RoutingRuleRepository routingRuleRepository;
    @Mock private NotificationChannelRepository channelRepository;
    @Mock private ChannelProviderRegistry providerRegistry;
    @Mock private DigestChannelRouter digestChannelRouter;
    @Mock private ChannelDeliveryLogRepository deliveryLogRepository;
    @Mock private ChannelDeliveryMetrics metrics;

    private RoutingRuleEvaluator evaluator;
    private ChannelRouter channelRouter;

    @BeforeEach
    void setUp() {
        evaluator = new RoutingRuleEvaluator();
        channelRouter = new ChannelRouter(
            routingRuleRepository, channelRepository, evaluator,
            providerRegistry, digestChannelRouter, deliveryLogRepository, metrics
        );
    }

    @Test
    void routeDeliversImmediatelyOnSuccess() {
        Notification notification = notification();
        RoutingRule rule = immediateRule(List.of(1L), false);
        NotificationChannel channel = activeChannel(ChannelType.SLACK);
        NotificationChannelProvider provider = mock(NotificationChannelProvider.class);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(eq(channel), any())).thenReturn(ChannelDeliveryResult.success("msg-123"));

        channelRouter.route(notification);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(logCaptor.getValue().getExternalMessageId()).isEqualTo("msg-123");
    }

    @Test
    void routeSavesRetryLogOnRetryableFailure() {
        Notification notification = notification();
        RoutingRule rule = immediateRule(List.of(1L), false);
        NotificationChannel channel = activeChannel(ChannelType.SLACK);
        NotificationChannelProvider provider = mock(NotificationChannelProvider.class);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(eq(channel), any())).thenReturn(ChannelDeliveryResult.failure("rate limited", true));

        channelRouter.route(notification);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(DeliveryStatus.RETRY);
    }

    @Test
    void routeSavesDeadLogOnNonRetryableFailure() {
        Notification notification = notification();
        RoutingRule rule = immediateRule(List.of(1L), false);
        NotificationChannel channel = activeChannel(ChannelType.SLACK);
        NotificationChannelProvider provider = mock(NotificationChannelProvider.class);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(eq(channel), any())).thenReturn(ChannelDeliveryResult.failure("invalid token", false));

        channelRouter.route(notification);

        ArgumentCaptor<ChannelDeliveryLog> logCaptor = ArgumentCaptor.forClass(ChannelDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(DeliveryStatus.DEAD);
    }

    @Test
    void routeQueuesDigestModeNotificationsToDigestRouter() {
        Notification notification = notification();
        RoutingRule rule = digestRule(List.of(1L), false, 60);
        NotificationChannel channel = activeChannel(ChannelType.SLACK);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel));

        channelRouter.route(notification);

        verify(digestChannelRouter).queue(notification, channel, rule);
        verify(providerRegistry, never()).get(any());
    }

    @Test
    void routeStopsAfterFirstMatchingRuleWhenStopOnMatchIsTrue() {
        Notification notification = notification();
        RoutingRule rule1 = immediateRule(List.of(1L), true);
        RoutingRule rule2 = immediateRule(List.of(2L), false);
        NotificationChannel channel1 = activeChannel(ChannelType.SLACK);
        NotificationChannelProvider provider = mock(NotificationChannelProvider.class);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule1, rule2));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel1));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(eq(channel1), any())).thenReturn(ChannelDeliveryResult.success("id"));

        channelRouter.route(notification);

        verify(channelRepository, never()).findAllById(List.of(2L));
    }

    @Test
    void routeSkipsDisabledRules() {
        Notification notification = notification();
        RoutingRule disabledRule = RoutingRule.builder()
            .userId(1L).ruleName("disabled").channelIds(List.of(1L))
            .conditions(RoutingCondition.empty()).isEnabled(false)
            .deliveryMode(DeliveryMode.IMMEDIATE).build();

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(disabledRule));

        channelRouter.route(notification);

        verify(channelRepository, never()).findAllById(any());
    }

    @Test
    void routeSkipsNonDeliverableChannels() {
        Notification notification = notification();
        RoutingRule rule = immediateRule(List.of(1L), false);
        NotificationChannel pausedChannel = NotificationChannel.builder()
            .id(1L).userId(1L).channelType(ChannelType.SLACK)
            .displayName("paused").credentialEncrypted("enc")
            .status(ChannelStatus.PAUSED).build();

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(pausedChannel));

        channelRouter.route(notification);

        verify(providerRegistry, never()).get(any());
        verify(deliveryLogRepository, never()).save(any());
    }

    @Test
    void buildMessageUsesAiSummaryWhenPresent() {
        Notification notification = Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.GITHUB)
            .title("Test").body("original body").priority(NotificationPriority.HIGH)
            .aiSummary("AI summary text").build();
        RoutingRule rule = immediateRule(List.of(1L), false);
        NotificationChannel channel = activeChannel(ChannelType.SLACK);
        NotificationChannelProvider provider = mock(NotificationChannelProvider.class);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(eq(channel), any())).thenReturn(ChannelDeliveryResult.success("id"));

        channelRouter.route(notification);

        ArgumentCaptor<ChannelMessage> messageCaptor = ArgumentCaptor.forClass(ChannelMessage.class);
        verify(provider).deliver(eq(channel), messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).isEqualTo("AI summary text");
    }

    @Test
    void buildMessageUsesBodyWhenAiSummaryIsNull() {
        Notification notification = notification();
        RoutingRule rule = immediateRule(List.of(1L), false);
        NotificationChannel channel = activeChannel(ChannelType.SLACK);
        NotificationChannelProvider provider = mock(NotificationChannelProvider.class);

        when(routingRuleRepository.findByUserIdOrderByPriorityOrder(1L)).thenReturn(List.of(rule));
        when(channelRepository.findAllById(List.of(1L))).thenReturn(List.of(channel));
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(eq(channel), any())).thenReturn(ChannelDeliveryResult.success("id"));

        channelRouter.route(notification);

        ArgumentCaptor<ChannelMessage> messageCaptor = ArgumentCaptor.forClass(ChannelMessage.class);
        verify(provider).deliver(eq(channel), messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).isEqualTo("Notification body");
    }

    @Test
    void computeNextRetryReturnsOneMinuteForFirstAttempt() {
        Instant before = Instant.now();
        Instant nextRetry = channelRouter.computeNextRetry(0);
        Instant after = Instant.now();

        assertThat(nextRetry).isBetween(
            before.plus(59, ChronoUnit.SECONDS),
            after.plus(61, ChronoUnit.SECONDS)
        );
    }

    @Test
    void computeNextRetryReturnsFiveMinutesForSecondAttempt() {
        Instant before = Instant.now();
        Instant nextRetry = channelRouter.computeNextRetry(1);
        Instant after = Instant.now();

        assertThat(nextRetry).isBetween(
            before.plus(4, ChronoUnit.MINUTES).plusSeconds(59),
            after.plus(5, ChronoUnit.MINUTES).plusSeconds(1)
        );
    }

    @Test
    void computeNextRetryReturnsTwentyFiveMinutesForThirdAttempt() {
        Instant before = Instant.now();
        Instant nextRetry = channelRouter.computeNextRetry(2);
        Instant after = Instant.now();

        assertThat(nextRetry).isBetween(
            before.plus(24, ChronoUnit.MINUTES).plusSeconds(59),
            after.plus(25, ChronoUnit.MINUTES).plusSeconds(1)
        );
    }

    @Test
    void computeNextRetryThrowsForExhaustedAttempts() {
        assertThatThrownBy(() -> channelRouter.computeNextRetry(3))
            .isInstanceOf(IllegalStateException.class);
    }

    private Notification notification() {
        return Notification.builder()
            .id(1L).userId(1L).source(NotificationSource.GITHUB)
            .title("Test notification").body("Notification body")
            .priority(NotificationPriority.HIGH).build();
    }

    private RoutingRule immediateRule(List<Long> channelIds, boolean stopOnMatch) {
        return RoutingRule.builder()
            .userId(1L).ruleName("test-rule")
            .conditions(RoutingCondition.empty())
            .channelIds(channelIds)
            .stopOnMatch(stopOnMatch)
            .isEnabled(true)
            .deliveryMode(DeliveryMode.IMMEDIATE).build();
    }

    private RoutingRule digestRule(List<Long> channelIds, boolean stopOnMatch, int intervalMin) {
        return RoutingRule.builder()
            .userId(1L).ruleName("digest-rule")
            .conditions(RoutingCondition.empty())
            .channelIds(channelIds)
            .stopOnMatch(stopOnMatch)
            .isEnabled(true)
            .deliveryMode(DeliveryMode.DIGEST)
            .digestIntervalMin(intervalMin).build();
    }

    private NotificationChannel activeChannel(ChannelType type) {
        return NotificationChannel.builder()
            .id(1L).userId(1L).channelType(type)
            .displayName("test-channel").credentialEncrypted("enc")
            .status(ChannelStatus.ACTIVE).build();
    }
}
