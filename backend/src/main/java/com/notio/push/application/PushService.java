package com.notio.push.application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.notio.notification.infrastructure.NotificationRepository;
import com.notio.push.api.RegisterDeviceRequest;
import com.notio.push.domain.Device;
import com.notio.push.infrastructure.DeviceRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PushService {

    private static final Logger logger = LoggerFactory.getLogger(PushService.class);

    private final DeviceRepository deviceRepository;
    private final NotificationRepository notificationRepository;

    @Value("${notio.firebase.enabled:false}")
    private boolean firebaseEnabled;

    public PushService(
            final DeviceRepository deviceRepository,
            final NotificationRepository notificationRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.notificationRepository = notificationRepository;
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
        if (!firebaseEnabled) {
            logger.debug("Firebase is disabled. Skipping push for notification {}", notificationId);
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            logger.warn("Firebase is not initialized. Skipping push for notification {}", notificationId);
            return;
        }

        final com.notio.notification.domain.Notification notificationEntity = notificationRepository.findById(notificationId)
                .orElse(null);

        if (notificationEntity == null) {
            logger.warn("Notification {} not found. Skipping push.", notificationId);
            return;
        }

        final List<Device> devices = deviceRepository.findAll();
        if (devices.isEmpty()) {
            logger.info("No devices registered. Skipping push for notification {}", notificationId);
            return;
        }

        final com.google.firebase.messaging.Notification notification = com.google.firebase.messaging.Notification.builder()
                .setTitle(notificationEntity.getTitle())
                .setBody(truncateBody(notificationEntity.getBody()))
                .build();

        for (Device device : devices) {
            try {
                final Message message = Message.builder()
                        .setToken(device.getToken())
                        .setNotification(notification)
                        .putData("notification_id", String.valueOf(notificationId))
                        .putData("source", notificationEntity.getSource().name())
                        .putData("priority", notificationEntity.getPriority().name())
                        .build();

                final String response = FirebaseMessaging.getInstance().send(message);
                logger.info("Successfully sent push to device {} for notification {}: {}",
                        device.getId(), notificationId, response);
            } catch (FirebaseMessagingException exception) {
                logger.error("Failed to send push to device {} for notification {}: {}",
                        device.getId(), notificationId, exception.getMessage());
            }
        }
    }

    private String truncateBody(final String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 197) + "..." : body;
    }
}

