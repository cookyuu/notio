package com.notio.push.dto;

import com.notio.push.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceRequest(
    @NotBlank(message = "FCM token is required")
    String fcmToken,

    @NotNull(message = "Platform is required")
    DevicePlatform platform,

    @NotBlank(message = "Device ID is required")
    String deviceId,

    String appVersion,

    String osVersion
) {
    public RegisterDeviceRequest {
        // 기본값 설정
        if (appVersion == null || appVersion.isBlank()) {
            appVersion = "1.0.0";
        }
        if (osVersion == null || osVersion.isBlank()) {
            osVersion = "unknown";
        }
    }
}
