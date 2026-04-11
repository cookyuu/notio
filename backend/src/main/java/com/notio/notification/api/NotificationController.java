package com.notio.notification.api;

import com.notio.common.api.ApiResponse;
import com.notio.common.api.PageMeta;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "20") final int size,
            @RequestParam(name = "source", required = false) final NotificationSource source,
            @RequestParam(name = "priority", required = false) final NotificationPriority priority,
            @RequestParam(name = "is_read", required = false) final Boolean isRead,
            @RequestParam(name = "keyword", required = false) final String keyword
    ) {
        final Page<NotificationResponse> responsePage = notificationService.findAll(
                        new NotificationFilterRequest(source, priority, isRead, keyword),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(NotificationResponse::from);
        return ApiResponse.success(
                responsePage.getContent(),
                new PageMeta(
                        responsePage.getNumber(),
                        responsePage.getSize(),
                        responsePage.getTotalElements(),
                        responsePage.getTotalPages()
                )
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getDetail(@PathVariable("id") final long id) {
        return ApiResponse.success(NotificationResponse.from(notificationService.getDetailAndMarkRead(id)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable("id") final long id) {
        return ApiResponse.success(NotificationResponse.from(notificationService.markRead(id)));
    }

    @PostMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse> markAllRead() {
        return ApiResponse.success(new NotificationReadAllResponse(notificationService.markAllRead()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success(new UnreadCountResponse(notificationService.countUnread()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<NotificationDeleteResponse> delete(@PathVariable("id") final long id) {
        notificationService.delete(id);
        return ApiResponse.success(new NotificationDeleteResponse(id, true));
    }
}
