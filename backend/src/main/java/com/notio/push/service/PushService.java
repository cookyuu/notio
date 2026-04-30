package com.notio.push.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.push.domain.Device;
import com.notio.push.repository.DeviceRepository;
import com.notio.notification.metrics.NotificationFlowMetrics;
import com.notio.notification.domain.Notification;
import com.notio.notification.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
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
    private final NotificationFlowMetrics notificationFlowMetrics;

    /**
     * 알림 ID로 푸시 발송
     */
    public void sendPush(Long notificationId, Long userId) {
        final Instant startedAt = Instant.now();
        // Firebase가 초기화되지 않은 경우 경고 로그만 출력
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("event=push_dispatch_failed notification_id={} reason=firebase_not_initialized", notificationId);
            notificationFlowMetrics.recordPushSend("failure", Duration.between(startedAt, Instant.now()));
            return;
        }

        Notification notification = notificationRepository.findByIdAndUserIdAndNotDeleted(userId, notificationId)
            .orElseThrow(() -> new NotioException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // Phase 0: 모든 활성 디바이스에 푸시 발송 (단일 사용자)
        List<Device> activeDevices = deviceRepository.findAllActive();

        if (activeDevices.isEmpty()) {
            log.info("event=push_dispatch_started notification_id={} device_count=0", notificationId);
            log.info("event=push_dispatch_succeeded notification_id={} device_count=0 success_count=0 failure_count=0", notificationId);
            notificationFlowMetrics.recordPushSend("success", Duration.between(startedAt, Instant.now()));
            return;
        }

        log.info("event=push_dispatch_started notification_id={} device_count={}", notificationId, activeDevices.size());
        sendPushToDevices(notification, activeDevices, startedAt);
    }

    /**
     * Phase 0 legacy callers. New notification flows should pass user id.
     */
    @Deprecated
    public void sendPush(Long notificationId) {
        sendPush(notificationId, 1L);
    }

    /**
     * 여러 디바이스에 푸시 발송
     */
    private void sendPushToDevices(Notification notification, List<Device> devices, Instant startedAt) {
        List<String> fcmTokens = devices.stream()
            .map(Device::getFcmToken)
            .collect(Collectors.toList());

        Message message = buildMessage(notification, fcmTokens.get(0));

        // 단일 토큰 발송
        if (fcmTokens.size() == 1) {
            sendSingleMessage(message, notification.getId(), startedAt);
            return;
        }

        // 멀티캐스트 발송
        MulticastMessage multicastMessage = buildMulticastMessage(notification, fcmTokens);
        sendMulticastMessage(multicastMessage, notification.getId(), fcmTokens.size(), startedAt);
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
    private void sendSingleMessage(Message message, Long notificationId, Instant startedAt) {
        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info(
                "event=push_dispatch_succeeded notification_id={} device_count=1 success_count=1 failure_count=0 response_id={}",
                notificationId,
                response
            );
            notificationFlowMetrics.recordPushSend("success", Duration.between(startedAt, Instant.now()));
        } catch (FirebaseMessagingException e) {
            log.error(
                "event=push_dispatch_failed notification_id={} device_count=1 success_count=0 failure_count=1 exception_type={}",
                notificationId,
                e.getClass().getSimpleName(),
                e
            );
            notificationFlowMetrics.recordPushSend("failure", Duration.between(startedAt, Instant.now()));
            // Phase 0에서는 푸시 실패해도 예외를 던지지 않음
        }
    }

    /**
     * 멀티캐스트 메시지 발송
     */
    private void sendMulticastMessage(MulticastMessage message, Long notificationId, int deviceCount, Instant startedAt) {
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            final String outcome = response.getFailureCount() > 0 ? "failure" : "success";
            log.info(
                "event={} notification_id={} device_count={} success_count={} failure_count={}",
                "success".equals(outcome) ? "push_dispatch_succeeded" : "push_dispatch_failed",
                notificationId,
                deviceCount,
                response.getSuccessCount(),
                response.getFailureCount()
            );
            notificationFlowMetrics.recordPushSend(outcome, Duration.between(startedAt, Instant.now()));

            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(sendResponse -> {
                    if (!sendResponse.isSuccessful()) {
                        log.warn(
                            "event=push_dispatch_failed notification_id={} device_count={} exception_type={}",
                            notificationId,
                            deviceCount,
                            sendResponse.getException().getClass().getSimpleName()
                        );
                    }
                });
            }
        } catch (FirebaseMessagingException e) {
            log.error(
                "event=push_dispatch_failed notification_id={} device_count={} exception_type={}",
                notificationId,
                deviceCount,
                e.getClass().getSimpleName(),
                e
            );
            notificationFlowMetrics.recordPushSend("failure", Duration.between(startedAt, Instant.now()));
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
