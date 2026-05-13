package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDigestSchedulerTest {

    @Mock private ChannelDeliveryLogRepository deliveryLogRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationChannelRepository channelRepository;
    @Mock private ChannelProviderRegistry providerRegistry;
    @Mock private PromptBuilder promptBuilder;
    @Mock private LlmProvider llmProvider;
    @Mock private ChannelDeliveryMetrics metrics;

    private NotificationDigestScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationDigestScheduler(
            deliveryLogRepository, notificationRepository, channelRepository,
            providerRegistry, promptBuilder, llmProvider, metrics
        );
    }

    @Test
    void processDigestsDeliversSuccessfullyAndUpdatesLogsToSuccess() {
        ChannelDeliveryLog log1 = pendingLog(1L, 1L);
        ChannelDeliveryLog log2 = pendingLog(2L, 1L);
        Notification n1 = notification(1L);
        Notification n2 = notification(2L);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log1, log2));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(n1, n2));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("묶음 요약 내용");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.success("ext-msg-id"));

        scheduler.processDigests();

        assertThat(log1.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log2.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log1.getExternalMessageId()).isEqualTo("ext-msg-id");
        verify(deliveryLogRepository).saveAll(anyList());
        verify(metrics).recordDigestDelivery(ChannelType.SLACK.name(), 2);
    }

    @Test
    void processDigestsUpdatesLogsToRetryOnRetryableFailure() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("rate limit", true));

        scheduler.processDigests();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.RETRY);
    }

    @Test
    void processDigestsUpdatesLogsToDeadOnNonRetryableFailure() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("bad webhook", false));

        scheduler.processDigests();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
    }

    @Test
    void processDigestsSetsLogsToDeadWhenNotificationsNotFound() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of());

        scheduler.processDigests();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
        verify(llmProvider, never()).chat(any());
    }

    @Test
    void processDigestsDoesNothingWhenNoExpiredPendingLogs() {
        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of());

        scheduler.processDigests();

        verify(notificationRepository, never()).findAllById(any());
        verify(llmProvider, never()).chat(any());
    }

    private ChannelDeliveryLog pendingLog(Long notificationId, Long channelId) {
        return ChannelDeliveryLog.builder()
            .notificationId(notificationId).channelId(channelId)
            .status(DeliveryStatus.DIGEST_PENDING)
            .nextRetryAt(Instant.now().minusSeconds(60)).build();
    }

    private Notification notification(Long id) {
        return Notification.builder()
            .id(id).userId(1L).source(NotificationSource.GITHUB)
            .title("Test " + id).body("Body " + id).priority(NotificationPriority.HIGH).build();
    }

    private NotificationChannel activeChannel() {
        return NotificationChannel.builder()
            .id(1L).userId(1L).channelType(ChannelType.SLACK)
            .displayName("test").credentialEncrypted("enc")
            .status(ChannelStatus.ACTIVE).build();
    }
}
