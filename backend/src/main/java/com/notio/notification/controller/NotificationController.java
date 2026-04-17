package com.notio.notification.controller;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.MarkAllReadResponse;
import com.notio.notification.dto.MarkReadResponse;
import com.notio.notification.dto.NotificationDetailResponse;
import com.notio.notification.dto.NotificationSummaryResponse;
import com.notio.notification.dto.UnreadCountResponse;
import com.notio.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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

    @Operation(
        summary = "알림 목록 조회",
        description = "현재 사용자 범위의 알림 목록을 조회합니다. 카드 렌더링에 필요한 요약 필드만 반환하며 필터링과 페이지네이션을 지원합니다."
    )
    @GetMapping
    public ApiResponse<Page<NotificationSummaryResponse>> getNotifications(
        @Parameter(description = "알림 소스 필터")
        @RequestParam(name = "source", required = false) NotificationSource source,

        @Parameter(description = "읽음 상태 필터")
        @RequestParam(name = "is_read", required = false) Boolean isRead,

        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(name = "page", defaultValue = "0") int page,

        @Parameter(description = "페이지 크기")
        @RequestParam(name = "size", defaultValue = "20") int size,

        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationSummaryResponse> response = notificationService.findAllSummaries(userId, source, isRead, pageable);

        return ApiResponse.success(response);
    }

    @Operation(
        summary = "알림 상세 조회",
        description = "현재 사용자 범위에서 특정 알림의 전체 본문과 부가 정보를 조회합니다. 조회 시 미읽음 알림은 자동으로 읽음 처리됩니다."
    )
    @GetMapping("/{id}")
    public ApiResponse<NotificationDetailResponse> getNotification(
        @Parameter(description = "알림 ID", required = true)
        @PathVariable("id") Long id,

        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        Notification notification = notificationService.getDetail(userId, id);
        NotificationDetailResponse response = NotificationDetailResponse.from(notification, notificationService);

        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{id}/read")
    public ApiResponse<MarkReadResponse> markAsRead(
        @Parameter(description = "알림 ID", required = true)
        @PathVariable("id") Long id,

        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        Notification notification = notificationService.markRead(userId, id);
        MarkReadResponse response = new MarkReadResponse(notification.getId(), notification.isRead());

        return ApiResponse.success(response);
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "모든 미읽음 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<MarkAllReadResponse> markAllAsRead(Authentication authentication) {
        Long userId = currentUserId(authentication);
        int count = notificationService.markAllRead(userId);
        MarkAllReadResponse response = new MarkAllReadResponse(count);

        return ApiResponse.success(response);
    }

    @Operation(summary = "미읽음 알림 수 조회", description = "미읽음 알림의 개수를 조회합니다. Redis 캐시를 사용하여 빠른 응답을 제공합니다.")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        Long userId = currentUserId(authentication);
        long count = notificationService.countUnread(userId);
        UnreadCountResponse response = new UnreadCountResponse(count);

        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
        @Parameter(description = "알림 ID", required = true)
        @PathVariable("id") Long id,

        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        notificationService.delete(userId, id);
        return ApiResponse.success(null);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException exception) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }
    }
}
