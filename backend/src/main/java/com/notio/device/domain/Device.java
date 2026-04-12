package com.notio.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_devices_fcm_token", columnList = "fcm_token", unique = true),
    @Index(name = "idx_devices_user_id", columnList = "user_id"),
    @Index(name = "idx_devices_is_active", columnList = "is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 500)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "app_version", nullable = false, length = 50)
    @Builder.Default
    private String appVersion = "1.0.0";

    @Column(name = "os_version", nullable = false, length = 50)
    @Builder.Default
    private String osVersion = "unknown";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // 비즈니스 메서드
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void updateToken(String newFcmToken) {
        this.fcmToken = newFcmToken;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.active = false;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isActive() {
        return active && !isDeleted();
    }
}
