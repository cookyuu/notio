package com.notio.channel.domain;

import com.notio.channel.converter.LongListConverter;
import com.notio.channel.converter.RoutingConditionConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routing_rules")
@SQLDelete(sql = "UPDATE routing_rules SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "priority_order", nullable = false)
    @Builder.Default
    private int priorityOrder = 100;

    @Column(name = "conditions", columnDefinition = "JSONB", nullable = false)
    @Convert(converter = RoutingConditionConverter.class)
    @Builder.Default
    private RoutingCondition conditions = RoutingCondition.empty();

    @Column(name = "channel_ids", columnDefinition = "JSONB", nullable = false)
    @Convert(converter = LongListConverter.class)
    @Builder.Default
    private List<Long> channelIds = new ArrayList<>();

    @Column(name = "stop_on_match", nullable = false)
    @Builder.Default
    private boolean stopOnMatch = true;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean isEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    @Builder.Default
    private DeliveryMode deliveryMode = DeliveryMode.IMMEDIATE;

    @Column(name = "digest_interval_min")
    private Integer digestIntervalMin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void update(
        String ruleName,
        int priorityOrder,
        RoutingCondition conditions,
        List<Long> channelIds,
        boolean stopOnMatch,
        boolean isEnabled,
        DeliveryMode deliveryMode,
        Integer digestIntervalMin
    ) {
        this.ruleName = ruleName;
        this.priorityOrder = priorityOrder;
        this.conditions = conditions;
        this.channelIds = channelIds;
        this.stopOnMatch = stopOnMatch;
        this.isEnabled = isEnabled;
        this.deliveryMode = deliveryMode;
        this.digestIntervalMin = digestIntervalMin;
    }

    public void updatePriorityOrder(int priorityOrder) {
        this.priorityOrder = priorityOrder;
    }
}
