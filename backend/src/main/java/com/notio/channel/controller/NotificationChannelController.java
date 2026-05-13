package com.notio.channel.controller;

import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.dto.ChannelResponse;
import com.notio.channel.dto.CreateChannelRequest;
import com.notio.channel.dto.UpdateChannelRequest;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.service.NotificationChannelService;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Channel", description = "채널 API")
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class NotificationChannelController {

    private final NotificationChannelService channelService;

    @Operation(summary = "채널 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChannelResponse> createChannel(
        @Valid @RequestBody CreateChannelRequest request,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        NotificationChannel channel = channelService.create(
            userId,
            request.channelType(),
            request.displayName(),
            request.credentialPlaintext(),
            request.targetIdentifier()
        );
        return ApiResponse.success(toResponse(channel));
    }

    @Operation(summary = "채널 목록 조회")
    @GetMapping
    public ApiResponse<List<ChannelResponse>> getChannels(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<ChannelResponse> responses = channelService.findAll(userId).stream()
            .map(this::toResponse)
            .toList();
        return ApiResponse.success(responses);
    }

    @Operation(summary = "채널 상세 조회")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<ChannelResponse> getChannel(
        @PathVariable("id") Long channelId,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        NotificationChannel channel = channelService.findById(userId, channelId);
        return ApiResponse.success(toResponse(channel));
    }

    @Operation(summary = "채널 수정")
    @PutMapping("/{id:\\d+}")
    public ApiResponse<ChannelResponse> updateChannel(
        @PathVariable("id") Long channelId,
        @Valid @RequestBody UpdateChannelRequest request,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        NotificationChannel channel = channelService.update(
            userId,
            channelId,
            request.displayName(),
            request.credentialPlaintext(),
            request.targetIdentifier()
        );
        return ApiResponse.success(toResponse(channel));
    }

    @Operation(summary = "채널 삭제")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> deleteChannel(
        @PathVariable("id") Long channelId,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        channelService.delete(userId, channelId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "채널 일시중지")
    @PatchMapping("/{id:\\d+}/pause")
    public ApiResponse<ChannelResponse> pauseChannel(
        @PathVariable("id") Long channelId,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        NotificationChannel channel = channelService.pause(userId, channelId);
        return ApiResponse.success(toResponse(channel));
    }

    @Operation(summary = "채널 재개")
    @PatchMapping("/{id:\\d+}/resume")
    public ApiResponse<ChannelResponse> resumeChannel(
        @PathVariable("id") Long channelId,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        NotificationChannel channel = channelService.resume(userId, channelId);
        return ApiResponse.success(toResponse(channel));
    }

    @Operation(summary = "채널 테스트 전송")
    @PostMapping("/{id:\\d+}/test")
    public ApiResponse<ChannelDeliveryResult> testChannel(
        @PathVariable("id") Long channelId,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        ChannelDeliveryResult result = channelService.test(userId, channelId);
        return ApiResponse.success(result);
    }

    private ChannelResponse toResponse(NotificationChannel channel) {
        return ChannelResponse.from(channel, channelService.getKeyPreview(channel));
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
