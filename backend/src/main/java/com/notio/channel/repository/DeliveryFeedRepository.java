package com.notio.channel.repository;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.dto.DeliveryFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.notio.channel.domain.ChannelDeliveryLog;

public interface DeliveryFeedRepository extends JpaRepository<ChannelDeliveryLog, Long> {

    @Query("""
        SELECT new com.notio.channel.dto.DeliveryFeedItem(
            cdl.id,
            n.id,
            n.title,
            nc.id,
            nc.channelType,
            nc.displayName,
            COALESCE(n.aiSummary, n.body),
            cdl.deliveredAt,
            cdl.status,
            cdl.externalMessageId
        )
        FROM ChannelDeliveryLog cdl
        JOIN Notification n ON n.id = cdl.notificationId
        JOIN NotificationChannel nc ON nc.id = cdl.channelId
        WHERE n.userId = :userId
          AND cdl.status = com.notio.channel.domain.DeliveryStatus.SUCCESS
          AND (:channelType IS NULL OR nc.channelType = :channelType)
        ORDER BY cdl.deliveredAt DESC
        """)
    Page<DeliveryFeedItem> findFeed(
        @Param("userId") Long userId,
        @Param("channelType") ChannelType channelType,
        Pageable pageable
    );
}
