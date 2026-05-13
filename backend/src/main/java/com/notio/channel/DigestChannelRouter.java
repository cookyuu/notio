package com.notio.channel;

import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.domain.RoutingRule;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigestChannelRouter {

    private final ChannelDeliveryLogRepository deliveryLogRepository;

    @Transactional
    public void queue(Notification notification, NotificationChannel channel, RoutingRule rule) {
        boolean windowExists = deliveryLogRepository
            .existsByChannelIdAndStatus(channel.getId(), DeliveryStatus.DIGEST_PENDING);

        Instant nextDeliveryAt = windowExists
            ? deliveryLogRepository
                .findMinNextRetryAtByChannelIdAndStatus(channel.getId(), DeliveryStatus.DIGEST_PENDING)
                .orElse(computeWindowEnd(rule))
            : computeWindowEnd(rule);

        ChannelDeliveryLog deliveryLog = ChannelDeliveryLog.builder()
            .notificationId(notification.getId())
            .channelId(channel.getId())
            .status(DeliveryStatus.DIGEST_PENDING)
            .nextRetryAt(nextDeliveryAt)
            .build();

        deliveryLogRepository.save(deliveryLog);
        log.info("event=digest_queued notification_id={} channel_id={} next_delivery_at={}",
            notification.getId(), channel.getId(), nextDeliveryAt);
    }

    private Instant computeWindowEnd(RoutingRule rule) {
        int intervalMin = rule.getDigestIntervalMin() != null ? rule.getDigestIntervalMin() : 30;
        return Instant.now().plus(intervalMin, ChronoUnit.MINUTES);
    }
}
