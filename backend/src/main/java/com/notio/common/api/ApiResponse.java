package com.notio.common.api;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Object meta
) {

    public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(final T data, final Object meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> error(final ApiError error) {
        return new ApiResponse<>(false, null, error, null);
    }
}
