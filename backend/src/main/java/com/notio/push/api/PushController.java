package com.notio.push.api;

import com.notio.common.api.ApiResponse;
import com.notio.push.application.PushService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class PushController {

    private final PushService pushService;

    public PushController(final PushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping("/register")
    public ApiResponse<DeviceResponse> register(@Valid @RequestBody final RegisterDeviceRequest request) {
        return ApiResponse.success(DeviceResponse.from(pushService.register(request)));
    }
}

