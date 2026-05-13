package com.notio.channel.repository;

import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChannelDeliveryLogRepository extends JpaRepository<ChannelDeliveryLog, Long> {

    boolean existsByChannelIdAndStatus(Long channelId, DeliveryStatus status);

    @Query("SELECT MIN(l.nextRetryAt) FROM ChannelDeliveryLog l WHERE l.channelId = :channelId AND l.status = :status")
    Optional<Instant> findMinNextRetryAtByChannelIdAndStatus(
        @Param("channelId") Long channelId,
        @Param("status") DeliveryStatus status
    );

    List<ChannelDeliveryLog> findTop50ByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant now);

    @Query("SELECT DISTINCT l.channelId FROM ChannelDeliveryLog l WHERE l.status = :status AND l.nextRetryAt <= :now")
    List<Long> findDistinctChannelIdsByStatusAndNextRetryAtBefore(
        @Param("status") DeliveryStatus status,
        @Param("now") Instant now
    );

    List<ChannelDeliveryLog> findByChannelIdAndStatusAndNextRetryAtBefore(
        Long channelId,
        DeliveryStatus status,
        Instant now
    );
}
