package com.notio.push.api;

import com.notio.push.domain.Device;

public record DeviceResponse(
        long id,
        String platform,
        String token,
        String deviceName
) {

    public static DeviceResponse from(final Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getPlatform(),
                device.getToken(),
                device.getDeviceName()
        );
    }
}

