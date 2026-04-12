package com.notio.push.application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.notio.common.error.ErrorCode;
import com.notio.common.error.NotioException;
import com.notio.device.domain.Device;
import com.notio.device.infrastructure.DeviceRepository;
import com.notio.notification.domain.Notification;
import com.notio.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushService {

    private final NotificationRepository notificationRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 알림 ID로 푸시 발송
     */
    public void sendPush(Long notificationId) {
        // Firebase가 초기화되지 않은 경우 경고 로그만 출력
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Skipping push notification for notificationId={}", notificationId);
            return;
        }

        Notification notification = notificationRepository.findByIdAndNotDeleted(notificationId)
            .orElseThrow(() -> new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // Phase 0: 모든 활성 디바이스에 푸시 발송 (단일 사용자)
        List<Device> activeDevices = deviceRepository.findAllActive();

        if (activeDevices.isEmpty()) {
            log.info("No active devices found. Skipping push notification for notificationId={}", notificationId);
            return;
        }

        sendPushToDevices(notification, activeDevices);
    }

    /**
     * 여러 디바이스에 푸시 발송
     */
    private void sendPushToDevices(Notification notification, List<Device> devices) {
        List<String> fcmTokens = devices.stream()
            .map(Device::getFcmToken)
            .collect(Collectors.toList());

        Message message = buildMessage(notification, fcmTokens.get(0));

        // 단일 토큰 발송
        if (fcmTokens.size() == 1) {
            sendSingleMessage(message, notification.getId());
            return;
        }

        // 멀티캐스트 발송
        MulticastMessage multicastMessage = buildMulticastMessage(notification, fcmTokens);
        sendMulticastMessage(multicastMessage, notification.getId(), fcmTokens.size());
    }

    /**
     * FCM 메시지 빌드
     */
    private Message buildMessage(Notification notification, String fcmToken) {
        return Message.builder()
            .setToken(fcmToken)
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build()
            )
            .putData("notificationId", notification.getId().toString())
            .putData("source", notification.getSource().name())
            .putData("priority", notification.getPriority().name())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(mapPriority(notification.getPriority()))
                    .setNotification(
                        AndroidNotification.builder()
                            .setSound("default")
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .build()
                    )
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setBadge(1)
                            .build()
                    )
                    .build()
            )
            .build();
    }

    /**
     * 멀티캐스트 메시지 빌드
     */
    private MulticastMessage buildMulticastMessage(Notification notification, List<String> fcmTokens) {
        return MulticastMessage.builder()
            .addAllTokens(fcmTokens)
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build()
            )
            .putData("notificationId", notification.getId().toString())
            .putData("source", notification.getSource().name())
            .putData("priority", notification.getPriority().name())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(mapPriority(notification.getPriority()))
                    .setNotification(
                        AndroidNotification.builder()
                            .setSound("default")
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .build()
                    )
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setBadge(1)
                            .build()
                    )
                    .build()
            )
            .build();
    }

    /**
     * 단일 메시지 발송
     */
    private void sendSingleMessage(Message message, Long notificationId) {
        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent successfully: notificationId={}, response={}",
                notificationId, response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification: notificationId={}, error={}",
                notificationId, e.getMessage(), e);
            // Phase 0에서는 푸시 실패해도 예외를 던지지 않음
        }
    }

    /**
     * 멀티캐스트 메시지 발송
     */
    private void sendMulticastMessage(MulticastMessage message, Long notificationId, int deviceCount) {
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Push notification sent to {} devices: notificationId={}, successCount={}, failureCount={}",
                deviceCount, notificationId, response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(sendResponse -> {
                    if (!sendResponse.isSuccessful()) {
                        log.warn("Failed to send push to device: error={}",
                            sendResponse.getException().getMessage());
                    }
                });
            }
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast push notification: notificationId={}, error={}",
                notificationId, e.getMessage(), e);
        }
    }

    /**
     * Notio 우선순위를 FCM 우선순위로 매핑
     */
    private AndroidConfig.Priority mapPriority(com.notio.notification.domain.NotificationPriority priority) {
        return switch (priority) {
            case URGENT, HIGH -> AndroidConfig.Priority.HIGH;
            case MEDIUM, LOW -> AndroidConfig.Priority.NORMAL;
        };
    }
}
