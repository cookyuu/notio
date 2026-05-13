package com.notio.channel.controller;

import com.notio.channel.domain.RoutingRule;
import com.notio.channel.dto.CreateRoutingRuleRequest;
import com.notio.channel.dto.ReorderRequest;
import com.notio.channel.dto.RoutingRuleResponse;
import com.notio.channel.dto.UpdateRoutingRuleRequest;
import com.notio.channel.service.RoutingRuleService;
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

@Tag(name = "RoutingRule", description = "라우팅 규칙 API")
@RestController
@RequestMapping("/api/v1/routing-rules")
@RequiredArgsConstructor
public class RoutingRuleController {

    private final RoutingRuleService routingRuleService;

    @Operation(summary = "라우팅 규칙 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoutingRuleResponse> createRule(
        @Valid @RequestBody CreateRoutingRuleRequest request,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        RoutingRule rule = routingRuleService.create(
            userId,
            request.ruleName(),
            request.priorityOrder(),
            request.conditions() != null ? request.conditions().toDomain() : null,
            request.channelIds(),
            request.stopOnMatch(),
            request.deliveryMode(),
            request.digestIntervalMin()
        );
        return ApiResponse.success(RoutingRuleResponse.from(rule));
    }

    @Operation(summary = "라우팅 규칙 목록 조회 (priority_order 오름차순)")
    @GetMapping
    public ApiResponse<List<RoutingRuleResponse>> getRules(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<RoutingRuleResponse> responses = routingRuleService.findAll(userId).stream()
            .map(RoutingRuleResponse::from)
            .toList();
        return ApiResponse.success(responses);
    }

    @Operation(summary = "라우팅 규칙 수정")
    @PutMapping("/{id:\\d+}")
    public ApiResponse<RoutingRuleResponse> updateRule(
        @PathVariable("id") Long ruleId,
        @Valid @RequestBody UpdateRoutingRuleRequest request,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        RoutingRule rule = routingRuleService.update(
            userId,
            ruleId,
            request.ruleName(),
            request.priorityOrder(),
            request.conditions() != null ? request.conditions().toDomain() : null,
            request.channelIds(),
            request.stopOnMatch(),
            request.isEnabled(),
            request.deliveryMode(),
            request.digestIntervalMin()
        );
        return ApiResponse.success(RoutingRuleResponse.from(rule));
    }

    @Operation(summary = "라우팅 규칙 삭제")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> deleteRule(
        @PathVariable("id") Long ruleId,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        routingRuleService.delete(userId, ruleId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "라우팅 규칙 순서 변경")
    @PatchMapping("/reorder")
    public ApiResponse<Void> reorderRules(
        @Valid @RequestBody ReorderRequest request,
        Authentication authentication
    ) {
        Long userId = currentUserId(authentication);
        routingRuleService.reorder(userId, request.orderedIds());
        return ApiResponse.success(null);
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
