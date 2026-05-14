package com.notio.channel.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_channels")
@SQLDelete(sql = "UPDATE notification_channels SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "credential_encrypted", nullable = false, columnDefinition = "TEXT")
    private String credentialEncrypted;

    @Column(name = "target_identifier", length = 255)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ChannelStatus status = ChannelStatus.ACTIVE;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private int errorCount = 0;

    @Column(name = "last_delivered_at")
    private Instant lastDeliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void recordSuccess() {
        this.status = ChannelStatus.ACTIVE;
        this.errorCount = 0;
        this.lastDeliveredAt = Instant.now();
    }

    public void recordFailure(String errorMsg) {
        this.errorCount++;
        this.lastError = errorMsg;
        if (this.errorCount >= 5) {
            this.status = ChannelStatus.ERROR;
        }
    }

    public boolean isDeliverable() {
        return this.status == ChannelStatus.ACTIVE;
    }

    public void pause() {
        this.status = ChannelStatus.PAUSED;
    }

    public void resume() {
        this.status = ChannelStatus.ACTIVE;
        this.errorCount = 0;
        this.lastError = null;
    }

    public void updateCredential(String credentialEncrypted, String targetIdentifier) {
        this.credentialEncrypted = credentialEncrypted;
        this.targetIdentifier = targetIdentifier;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
