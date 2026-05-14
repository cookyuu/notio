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

        log.debug("event=routing_start notification_id={} user_id={} rule_count={}",
            notification.getId(), notification.getUserId(), rules.size());

        for (RoutingRule rule : rules) {
            if (!rule.isEnabled()) {
                log.debug("event=routing_rule_skipped reason=disabled rule_id={} rule_name={} notification_id={}",
                    rule.getId(), rule.getRuleName(), notification.getId());
                continue;
            }
            if (!evaluator.matches(rule, notification)) {
                log.debug("event=routing_rule_skipped reason=condition_mismatch rule_id={} rule_name={} notification_id={} source={} priority={}",
                    rule.getId(), rule.getRuleName(), notification.getId(),
                    notification.getSource(), notification.getPriority());
                continue;
            }

            List<NotificationChannel> allChannels = channelRepository.findAllById(rule.getChannelIds());
            List<NotificationChannel> channels = allChannels.stream()
                .filter(NotificationChannel::isDeliverable)
                .toList();

            if (channels.isEmpty()) {
                log.warn("event=routing_no_deliverable_channels rule_id={} rule_name={} notification_id={} registered={} non_deliverable={}",
                    rule.getId(), rule.getRuleName(), notification.getId(),
                    allChannels.size(),
                    allChannels.stream().map(ch -> ch.getId() + ":" + ch.getStatus()).toList());
                continue;
            }

            if (rule.getDeliveryMode() == DeliveryMode.DIGEST) {
                channels.forEach(ch -> digestChannelRouter.queue(notification, ch, rule));
            } else {
                channels.forEach(ch -> deliverImmediate(notification, ch));
            }

            log.info("event=routing_rule_applied rule_id={} rule_name={} notification_id={} channel_count={} stop_on_match={}",
                rule.getId(), rule.getRuleName(), notification.getId(),
                channels.size(), rule.isStopOnMatch());

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
                log.info("event=channel_delivery_success channel_id={} channel_type={} notification_id={}",
                    channel.getId(), channel.getChannelType(), notification.getId());
            } else {
                channel.recordFailure(result.errorMessage());
                DeliveryStatus nextStatus = result.retryable()
                    ? DeliveryStatus.RETRY : DeliveryStatus.DEAD;
                Instant nextRetry = result.retryable() ? computeNextRetry(0) : null;
                saveLog(notification.getId(), channel.getId(),
                    nextStatus, null, result.errorMessage(), null, nextRetry);
                metrics.recordDelivery(channel.getChannelType().name(), "failure", duration);
                log.warn("event=channel_delivery_failed channel_id={} channel_type={} notification_id={} error={} retryable={} error_count={}",
                    channel.getId(), channel.getChannelType(), notification.getId(),
                    result.errorMessage(), result.retryable(), channel.getErrorCount());
            }
        } catch (Exception e) {
            log.error("event=channel_delivery_exception channel_id={} channel_type={} notification_id={}",
                channel.getId(), channel.getChannelType(), notification.getId(), e);
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
