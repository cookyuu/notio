package com.notio.common.api;

public record ApiError(
        String code,
        String message
) {
}
