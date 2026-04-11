package com.notio.push.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank @Size(max = 50) String platform,
        @NotBlank @Size(max = 500) String token,
        @Size(max = 255) String deviceName
) {
}

