package com.campus.events.common;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data, RequestIds.current());
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(code, message, null, RequestIds.current());
    }
}
