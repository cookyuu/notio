package com.notio.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationSource source;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority;

    @Column(nullable = false)
    private boolean isRead;

    @Column(length = 255)
    private String externalId;

    @Column(length = 1000)
    private String externalUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    protected Notification() {
    }

    public Notification(
            final NotificationSource source,
            final String title,
            final String body,
            final NotificationPriority priority,
            final String externalId,
            final String externalUrl,
            final Map<String, Object> metadata
    ) {
        this.source = source;
        this.title = title;
        this.body = body;
        this.priority = priority;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        this.isRead = false;
    }

    @PrePersist
    void onCreate() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void markRead() {
        isRead = true;
    }

    public void markDeleted() {
        deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public NotificationSource getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}

