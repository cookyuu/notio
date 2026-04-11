package com.notio.push.application;

import com.notio.push.api.RegisterDeviceRequest;
import com.notio.push.domain.Device;
import com.notio.push.infrastructure.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushService {

    private static final Logger logger = LoggerFactory.getLogger(PushService.class);

    private final DeviceRepository deviceRepository;

    public PushService(final DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Device register(final RegisterDeviceRequest request) {
        return deviceRepository.findByToken(request.token())
                .map(device -> {
                    device.update(request.platform(), request.deviceName());
                    return device;
                })
                .orElseGet(() -> deviceRepository.save(
                        new Device(request.platform(), request.token(), request.deviceName())
                ));
    }

    public void sendPush(final long notificationId) {
        logger.info("Skipping actual push delivery for notification {}", notificationId);
    }
}

