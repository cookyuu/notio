package com.notio.channel.controller;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.dto.DeliveryFeedItem;
import com.notio.channel.service.DeliveryFeedService;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Channel", description = "채널 API")
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class DeliveryFeedController {

    private final DeliveryFeedService deliveryFeedService;

    @Operation(summary = "전달 피드 조회", description = "성공적으로 전달된 알림 목록을 조회합니다.")
    @GetMapping("/delivery-feed")
    public ApiResponse<Page<DeliveryFeedItem>> getDeliveryFeed(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) ChannelType channelType,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
            Sort.by("deliveredAt").descending());
        Page<DeliveryFeedItem> feed = deliveryFeedService.getFeed(userId, channelType, pageable);
        return ApiResponse.success(feed);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new NotioException(ErrorCode.UNAUTHORIZED);
        }
    }
}
