package com.rhoonart.unearth.user.exception;

import com.rhoonart.unearth.common.BaseException;
import com.rhoonart.unearth.common.ResponseCode;

public class LoginException extends BaseException {
    public LoginException(String message) {
        super(ResponseCode.AUTH_FAIL, message);
    }
}