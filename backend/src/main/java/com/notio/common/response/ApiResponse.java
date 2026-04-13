package com.notio.common.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error
) {

    public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(final ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
