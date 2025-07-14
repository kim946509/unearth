package com.rhoonart.unearth.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class CommonResponse<T> {
    private final ResponseCode code;
    private final String message;
    private final T data;

    public static <T> CommonResponse<T> success(T data) {
        return CommonResponse.of(ResponseCode.SUCCESS, ResponseCode.SUCCESS.getMessage(), data);
    }

    public static <T> CommonResponse<T> fail(ResponseCode code) {
        return CommonResponse.of(code, code.getMessage(), null);
    }

    public static <T> CommonResponse<T> fail(ResponseCode code, String customMessage) {
        return CommonResponse.of(code, customMessage, null);
    }
}