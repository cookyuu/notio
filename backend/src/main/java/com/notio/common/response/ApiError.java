package com.notio.common.response;

public record ApiError(
        String code,
        String message
) {
}
