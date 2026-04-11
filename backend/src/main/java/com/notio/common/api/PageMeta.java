package com.notio.common.api;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

