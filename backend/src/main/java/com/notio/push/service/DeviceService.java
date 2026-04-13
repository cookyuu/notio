package com.notio.push.service;

import com.notio.push.domain.Device;
import com.notio.push.domain.DevicePlatform;
import com.notio.push.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    /**
     * 디바이스 등록 또는 업데이트
     * - 동일한 deviceId가 있으면 FCM 토큰만 업데이트
     * - 없으면 새로 생성
     */
    @Transactional
    public Device registerOrUpdate(
        String fcmToken,
        DevicePlatform platform,
        String deviceId,
        String appVersion,
        String osVersion
    ) {
        // deviceId로 기존 디바이스 찾기
        Optional<Device> existingDevice = deviceRepository.findByDeviceId(deviceId);

        if (existingDevice.isPresent()) {
            Device device = existingDevice.get();
            device.updateToken(fcmToken);
            device.activate();
            log.info("Device token updated: deviceId={}, fcmToken={}", deviceId, fcmToken);
            return device;
        }

        // 새 디바이스 생성
        Device newDevice = Device.builder()
            .fcmToken(fcmToken)
            .platform(platform)
            .deviceId(deviceId)
            .appVersion(appVersion)
            .osVersion(osVersion)
            .active(true)
            .build();

        Device saved = deviceRepository.save(newDevice);
        log.info("New device registered: id={}, deviceId={}, platform={}",
            saved.getId(), deviceId, platform);

        return saved;
    }

    /**
     * 디바이스 비활성화
     */
    @Transactional
    public void deactivate(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.deactivate();
            log.info("Device deactivated: deviceId={}", deviceId);
        });
    }
}
