package com.earseo.core.common;

public record BaseResponse<T>(
        String status,
        String message,
        T data
) {
    public static <T> BaseResponse<T> ok(T result) {
        return new BaseResponse<>("OK", "요청이 성공적으로 처리되었습니다.", result);
    }

    public static <T> BaseResponse<T> onFailure(String status, String message, T data) {
        return new BaseResponse<>(status, message, data);
    }
}
