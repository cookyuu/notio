package com.notio.connection.controller;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import com.notio.connection.dto.ConnectionActionResponse;
import com.notio.connection.dto.ConnectionResponse;
import com.notio.connection.dto.ConnectionSecretResponse;
import com.notio.connection.dto.CreateConnectionRequest;
import com.notio.connection.service.ConnectionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @GetMapping
    public ApiResponse<List<ConnectionResponse>> findAll(final Authentication authentication) {
        return ApiResponse.success(connectionService.findAll(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConnectionResponse> findById(
        @PathVariable("id") final Long id,
        final Authentication authentication
    ) {
        return ApiResponse.success(connectionService.findById(currentUserId(authentication), id));
    }

    @PostMapping
    public ApiResponse<ConnectionSecretResponse> create(
        @Valid @RequestBody final CreateConnectionRequest request,
        final Authentication authentication
    ) {
        return ApiResponse.success(connectionService.create(currentUserId(authentication), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @PathVariable("id") final Long id,
        final Authentication authentication
    ) {
        connectionService.delete(currentUserId(authentication), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<ConnectionActionResponse> test(
        @PathVariable("id") final Long id,
        final Authentication authentication
    ) {
        return ApiResponse.success(connectionService.test(currentUserId(authentication), id));
    }

    @PostMapping("/{id}/refresh")
    public ApiResponse<ConnectionActionResponse> refresh(
        @PathVariable("id") final Long id,
        final Authentication authentication
    ) {
        return ApiResponse.success(connectionService.refresh(currentUserId(authentication), id));
    }

    @PostMapping("/{id}/rotate-key")
    public ApiResponse<ConnectionSecretResponse> rotateKey(
        @PathVariable("id") final Long id,
        final Authentication authentication
    ) {
        return ApiResponse.success(connectionService.rotateKey(currentUserId(authentication), id));
    }

    private Long currentUserId(final Authentication authentication) {
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
