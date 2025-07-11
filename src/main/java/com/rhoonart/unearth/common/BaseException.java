package com.rhoonart.unearth.common;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final ResponseCode code;
    private final String message;

    public BaseException(ResponseCode code) {
        super(code.getMessage());
        this.code = code;
        this.message = code.getMessage();
    }

    public BaseException(ResponseCode code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}