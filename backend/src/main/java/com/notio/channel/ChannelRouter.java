package com.notio.channel;

import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.DeliveryMode;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.domain.RoutingRule;
import com.notio.channel.metrics.ChannelDeliveryMetrics;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.provider.ChannelMessage;
import com.notio.channel.provider.NotificationChannelProvider;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.channel.repository.NotificationChannelRepository;
import com.notio.channel.repository.RoutingRuleRepository;
import com.notio.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRouter {

    private final RoutingRuleRepository routingRuleRepository;
    private final NotificationChannelRepository channelRepository;
    private final RoutingRuleEvaluator evaluator;
    private final ChannelProviderRegistry providerRegistry;
    private final DigestChannelRouter digestChannelRouter;
    private final ChannelDeliveryLogRepository deliveryLogRepository;
    private final ChannelDeliveryMetrics metrics;

    @Transactional
    public void route(Notification notification) {
        List<RoutingRule> rules = routingRuleRepository
            .findByUserIdOrderByPriorityOrder(notification.getUserId());

        for (RoutingRule rule : rules) {
            if (!rule.isEnabled() || !evaluator.matches(rule, notification)) {
                continue;
            }

            List<NotificationChannel> channels = channelRepository
                .findAllById(rule.getChannelIds())
                .stream()
                .filter(NotificationChannel::isDeliverable)
                .toList();

            if (rule.getDeliveryMode() == DeliveryMode.DIGEST) {
                channels.forEach(ch -> digestChannelRouter.queue(notification, ch, rule));
            } else {
                channels.forEach(ch -> deliverImmediate(notification, ch));
            }

            if (rule.isStopOnMatch()) {
                break;
            }
        }
    }

    private void deliverImmediate(Notification notification, NotificationChannel channel) {
        ChannelMessage message = buildMessage(notification);
        NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
        Instant start = Instant.now();
        try {
            ChannelDeliveryResult result = provider.deliver(channel, message);
            Duration duration = Duration.between(start, Instant.now());

            if (result.success()) {
                channel.recordSuccess();
                saveLog(notification.getId(), channel.getId(),
                    DeliveryStatus.SUCCESS, result.externalMessageId(), null, Instant.now(), null);
                metrics.recordDelivery(channel.getChannelType().name(), "success", duration);
            } else {
                channel.recordFailure(result.errorMessage());
                DeliveryStatus nextStatus = result.retryable()
                    ? DeliveryStatus.RETRY : DeliveryStatus.DEAD;
                Instant nextRetry = result.retryable() ? computeNextRetry(0) : null;
                saveLog(notification.getId(), channel.getId(),
                    nextStatus, null, result.errorMessage(), null, nextRetry);
                metrics.recordDelivery(channel.getChannelType().name(), "failure", duration);
            }
        } catch (Exception e) {
            log.error("event=channel_delivery_exception channel_id={} notification_id={}",
                channel.getId(), notification.getId(), e);
            saveLog(notification.getId(), channel.getId(),
                DeliveryStatus.RETRY, null, e.getMessage(), null, computeNextRetry(0));
        }
    }

    private void saveLog(
        Long notificationId,
        Long channelId,
        DeliveryStatus status,
        String externalMessageId,
        String lastError,
        Instant deliveredAt,
        Instant nextRetryAt
    ) {
        ChannelDeliveryLog log = ChannelDeliveryLog.builder()
            .notificationId(notificationId)
            .channelId(channelId)
            .status(status)
            .externalMessageId(externalMessageId)
            .lastError(lastError)
            .deliveredAt(deliveredAt)
            .nextRetryAt(nextRetryAt)
            .build();
        deliveryLogRepository.save(log);
    }

    private ChannelMessage buildMessage(Notification notification) {
        String body = notification.getAiSummary() != null
            ? notification.getAiSummary()
            : notification.getBody();
        return new ChannelMessage(
            notification.getId(),
            notification.getTitle(),
            body,
            notification.getSource(),
            notification.getPriority(),
            notification.getExternalUrl(),
            notification.getCreatedAt()
        );
    }

    Instant computeNextRetry(int attemptCount) {
        long minutes = switch (attemptCount) {
            case 0 -> 1;
            case 1 -> 5;
            case 2 -> 25;
            default -> throw new IllegalStateException("Max retries exceeded: attemptCount=" + attemptCount);
        };
        return Instant.now().plus(minutes, ChronoUnit.MINUTES);
    }
}
