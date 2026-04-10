package com.notio.device.api;

import com.notio.device.domain.Device;
import com.notio.device.domain.DevicePlatform;

import java.time.Instant;

public record DeviceResponse(
    Long id,
    String deviceId,
    DevicePlatform platform,
    String appVersion,
    String osVersion,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
            device.getId(),
            device.getDeviceId(),
            device.getPlatform(),
            device.getAppVersion(),
            device.getOsVersion(),
            device.isActive(),
            device.getCreatedAt(),
            device.getUpdatedAt()
        );
    }
}
