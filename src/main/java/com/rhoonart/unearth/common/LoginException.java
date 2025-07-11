package com.rhoonart.unearth.common;

public class LoginException extends BaseException {
    public LoginException(String message) {
        super(ResponseCode.AUTH_FAIL, message);
    }
}