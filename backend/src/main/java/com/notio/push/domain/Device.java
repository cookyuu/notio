package com.notio.push.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(length = 255)
    private String deviceName;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    protected Device() {
    }

    public Device(final String platform, final String token, final String deviceName) {
        this.platform = platform;
        this.token = token;
        this.deviceName = deviceName;
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

    public void update(final String platformValue, final String deviceNameValue) {
        platform = platformValue;
        deviceName = deviceNameValue;
        deletedAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getPlatform() {
        return platform;
    }

    public String getToken() {
        return token;
    }

    public String getDeviceName() {
        return deviceName;
    }
}

