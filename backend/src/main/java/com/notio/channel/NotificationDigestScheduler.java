package com.notio.channel;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.metrics.ChannelDeliveryMetrics;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.provider.ChannelMessage;
import com.notio.channel.provider.NotificationChannelProvider;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.channel.repository.NotificationChannelRepository;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDigestScheduler {

    private final ChannelDeliveryLogRepository deliveryLogRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelRepository channelRepository;
    private final ChannelProviderRegistry providerRegistry;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final ChannelDeliveryMetrics metrics;

    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void processDigests() {
        List<Long> channelIds = deliveryLogRepository
            .findDistinctChannelIdsByStatusAndNextRetryAtBefore(DeliveryStatus.DIGEST_PENDING, Instant.now());

        channelIds.forEach(this::processDigestForChannel);
    }

    private void processDigestForChannel(Long channelId) {
        List<ChannelDeliveryLog> pendingLogs = deliveryLogRepository
            .findByChannelIdAndStatusAndNextRetryAtBefore(
                channelId, DeliveryStatus.DIGEST_PENDING, Instant.now());

        if (pendingLogs.isEmpty()) {
            return;
        }

        List<Long> notificationIds = pendingLogs.stream()
            .map(ChannelDeliveryLog::getNotificationId).toList();
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);

        if (notifications.isEmpty()) {
            pendingLogs.forEach(l -> l.setStatus(DeliveryStatus.DEAD));
            deliveryLogRepository.saveAll(pendingLogs);
            return;
        }

        NotificationChannel channel = channelRepository.findById(channelId)
            .orElseGet(() -> {
                log.warn("event=digest_channel_not_found channel_id={}", channelId);
                return null;
            });

        if (channel == null) {
            pendingLogs.forEach(l -> l.setStatus(DeliveryStatus.DEAD));
            deliveryLogRepository.saveAll(pendingLogs);
            return;
        }

        try {
            LlmPrompt prompt = promptBuilder.buildDigestSummaryPrompt(notifications);
            String digestContent = llmProvider.chat(prompt);

            ChannelMessage digestMessage = new ChannelMessage(
                notifications.get(0).getId(),
                "[묶음 알림] " + notifications.size() + "개",
                digestContent,
                notifications.get(0).getSource(),
                NotificationPriority.MEDIUM,
                null,
                Instant.now()
            );

            NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
            ChannelDeliveryResult result = provider.deliver(channel, digestMessage);

            Instant now = Instant.now();
            if (result.success()) {
                pendingLogs.forEach(l -> {
                    l.setStatus(DeliveryStatus.SUCCESS);
                    l.setDeliveredAt(now);
                    l.setExternalMessageId(result.externalMessageId());
                });
                channel.recordSuccess();
                channelRepository.save(channel);
                metrics.recordDigestDelivery(channel.getChannelType().name(), notifications.size());
                log.info("event=digest_delivered channel_id={} count={}", channelId, notifications.size());
            } else {
                pendingLogs.forEach(l -> {
                    l.setStatus(result.retryable() ? DeliveryStatus.RETRY : DeliveryStatus.DEAD);
                    l.setLastError(result.errorMessage());
                });
                channel.recordFailure(result.errorMessage());
                channelRepository.save(channel);
                log.warn("event=digest_delivery_failed channel_id={} retryable={} error={}",
                    channelId, result.retryable(), result.errorMessage());
            }
            deliveryLogRepository.saveAll(pendingLogs);

        } catch (Exception e) {
            log.error("event=digest_processing_failed channel_id={}", channelId, e);
        }
    }
}
