package com.notio.todo.domain;

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

@Entity
@Table(name = "todos")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TodoStatus status;

    @Column
    private Long notificationId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    protected Todo() {
    }

    public Todo(
            final String title,
            final String description,
            final TodoStatus status,
            final Long notificationId
    ) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.notificationId = notificationId;
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

    public void update(final String newTitle, final String newDescription, final TodoStatus newStatus) {
        if (newTitle != null && !newTitle.isBlank()) {
            title = newTitle;
        }
        if (newDescription != null) {
            description = newDescription;
        }
        if (newStatus != null) {
            status = newStatus;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TodoStatus getStatus() {
        return status;
    }

    public Long getNotificationId() {
        return notificationId;
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

