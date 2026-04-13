package com.notio.notification.controller;

import com.notio.common.response.ApiResponse;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.MarkAllReadResponse;
import com.notio.notification.dto.MarkReadResponse;
import com.notio.notification.dto.NotificationResponse;
import com.notio.notification.dto.UnreadCountResponse;
import com.notio.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다. 필터링 및 페이지네이션을 지원합니다.")
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
        @Parameter(description = "알림 소스 필터")
        @RequestParam(required = false) NotificationSource source,

        @Parameter(description = "읽음 상태 필터")
        @RequestParam(name = "is_read", required = false) Boolean isRead,

        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notifications = notificationService.findAll(source, isRead, pageable);
        Page<NotificationResponse> response = notifications.map(n ->
            NotificationResponse.from(n, notificationService));

        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 상세 조회", description = "특정 알림의 상세 정보를 조회합니다. 조회 시 자동으로 읽음 처리됩니다.")
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getNotification(
        @Parameter(description = "알림 ID", required = true)
        @PathVariable Long id
    ) {
        Notification notification = notificationService.markRead(id);  // 조회 시 자동 읽음 처리
        NotificationResponse response = NotificationResponse.from(notification, notificationService);

        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{id}/read")
    public ApiResponse<MarkReadResponse> markAsRead(
        @Parameter(description = "알림 ID", required = true)
        @PathVariable Long id
    ) {
        Notification notification = notificationService.markRead(id);
        MarkReadResponse response = new MarkReadResponse(notification.getId(), notification.isRead());

        return ApiResponse.success(response);
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "모든 미읽음 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<MarkAllReadResponse> markAllAsRead() {
        int count = notificationService.markAllRead();
        MarkAllReadResponse response = new MarkAllReadResponse(count);

        return ApiResponse.success(response);
    }

    @Operation(summary = "미읽음 알림 수 조회", description = "미읽음 알림의 개수를 조회합니다. Redis 캐시를 사용하여 빠른 응답을 제공합니다.")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        long count = notificationService.countUnread();
        UnreadCountResponse response = new UnreadCountResponse(count);

        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
        @Parameter(description = "알림 ID", required = true)
        @PathVariable Long id
    ) {
        notificationService.delete(id);
        return ApiResponse.success(null);
    }
}
