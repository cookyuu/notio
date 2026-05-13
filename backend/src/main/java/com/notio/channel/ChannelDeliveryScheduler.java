package com.notio.channel;

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
import com.notio.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelDeliveryScheduler {

    private static final int MAX_ATTEMPT_COUNT = 3;

    private final ChannelDeliveryLogRepository deliveryLogRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelRepository channelRepository;
    private final ChannelProviderRegistry providerRegistry;
    private final ChannelRouter channelRouter;
    private final ChannelDeliveryMetrics metrics;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void processRetries() {
        List<ChannelDeliveryLog> retryLogs = deliveryLogRepository
            .findTop50ByStatusAndNextRetryAtBefore(DeliveryStatus.RETRY, Instant.now());

        if (!retryLogs.isEmpty()) {
            log.info("event=retry_batch_start count={}", retryLogs.size());
        }

        retryLogs.forEach(this::retryDelivery);
    }

    private void retryDelivery(ChannelDeliveryLog deliveryLog) {
        int nextAttempt = deliveryLog.getAttemptCount() + 1;

        if (nextAttempt >= MAX_ATTEMPT_COUNT) {
            deliveryLog.setStatus(DeliveryStatus.DEAD);
            deliveryLog.setAttemptCount(nextAttempt);
            deliveryLogRepository.save(deliveryLog);

            channelRepository.findById(deliveryLog.getChannelId())
                .ifPresent(ch -> metrics.recordDead(ch.getChannelType().name()));

            log.warn("event=delivery_dead log_id={} notification_id={} channel_id={}",
                deliveryLog.getId(), deliveryLog.getNotificationId(), deliveryLog.getChannelId());
            return;
        }

        NotificationChannel channel = channelRepository.findById(deliveryLog.getChannelId())
            .orElseGet(() -> {
                log.warn("event=retry_channel_not_found channel_id={}", deliveryLog.getChannelId());
                return null;
            });

        if (channel == null || !channel.isDeliverable()) {
            deliveryLog.setStatus(DeliveryStatus.DEAD);
            deliveryLogRepository.save(deliveryLog);
            return;
        }

        Notification notification = notificationRepository.findById(deliveryLog.getNotificationId())
            .orElseGet(() -> {
                log.warn("event=retry_notification_not_found notification_id={}", deliveryLog.getNotificationId());
                return null;
            });

        if (notification == null) {
            deliveryLog.setStatus(DeliveryStatus.DEAD);
            deliveryLogRepository.save(deliveryLog);
            return;
        }

        String body = notification.getAiSummary() != null
            ? notification.getAiSummary()
            : notification.getBody();
        ChannelMessage message = new ChannelMessage(
            notification.getId(),
            notification.getTitle(),
            body,
            notification.getSource(),
            notification.getPriority(),
            notification.getExternalUrl(),
            notification.getCreatedAt()
        );

        NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
        Instant start = Instant.now();

        try {
            ChannelDeliveryResult result = provider.deliver(channel, message);
            Duration duration = Duration.between(start, Instant.now());
            deliveryLog.setAttemptCount(nextAttempt);

            if (result.success()) {
                deliveryLog.setStatus(DeliveryStatus.SUCCESS);
                deliveryLog.setExternalMessageId(result.externalMessageId());
                deliveryLog.setDeliveredAt(Instant.now());
                deliveryLog.setNextRetryAt(null);
                channel.recordSuccess();
                metrics.recordDelivery(channel.getChannelType().name(), "success", duration);
                log.info("event=retry_success log_id={} notification_id={}",
                    deliveryLog.getId(), deliveryLog.getNotificationId());
            } else {
                channel.recordFailure(result.errorMessage());
                deliveryLog.setLastError(result.errorMessage());
                if (result.retryable()) {
                    deliveryLog.setStatus(DeliveryStatus.RETRY);
                    deliveryLog.setNextRetryAt(channelRouter.computeNextRetry(nextAttempt));
                    metrics.recordRetry(channel.getChannelType().name());
                } else {
                    deliveryLog.setStatus(DeliveryStatus.DEAD);
                    metrics.recordDead(channel.getChannelType().name());
                }
                metrics.recordDelivery(channel.getChannelType().name(), "failure", duration);
            }
        } catch (Exception e) {
            log.error("event=retry_exception log_id={}", deliveryLog.getId(), e);
            deliveryLog.setAttemptCount(nextAttempt);
            deliveryLog.setLastError(e.getMessage());
            deliveryLog.setStatus(DeliveryStatus.RETRY);
            deliveryLog.setNextRetryAt(channelRouter.computeNextRetry(Math.min(nextAttempt, 2)));
        }

        deliveryLogRepository.save(deliveryLog);
        channelRepository.save(channel);
    }
}
