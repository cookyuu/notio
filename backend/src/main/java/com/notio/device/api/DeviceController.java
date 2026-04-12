package com.notio.device.api;

import com.notio.common.api.ApiResponse;
import com.notio.device.application.DeviceService;
import com.notio.device.domain.Device;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Device", description = "디바이스 관리 API")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(
        summary = "디바이스 등록",
        description = "FCM 토큰을 등록합니다. 동일한 deviceId가 있으면 토큰을 업데이트합니다."
    )
    @PostMapping("/register")
    public ApiResponse<DeviceResponse> registerDevice(
        @Valid @RequestBody RegisterDeviceRequest request
    ) {
        Device device = deviceService.registerOrUpdate(
            request.fcmToken(),
            request.platform(),
            request.deviceId(),
            request.appVersion(),
            request.osVersion()
        );

        return ApiResponse.success(DeviceResponse.from(device));
    }

    @Operation(
        summary = "디바이스 비활성화",
        description = "디바이스를 비활성화하여 푸시 알림 수신을 중지합니다."
    )
    @PatchMapping("/{deviceId}/deactivate")
    public ApiResponse<Void> deactivateDevice(
        @Parameter(description = "디바이스 ID", required = true)
        @PathVariable String deviceId
    ) {
        deviceService.deactivate(deviceId);
        return ApiResponse.success(null);
    }
}
