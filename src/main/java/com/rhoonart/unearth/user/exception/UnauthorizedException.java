package com.rhoonart.unearth.user.exception;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(ResponseCode.AUTH_FAIL, message);
    }
}