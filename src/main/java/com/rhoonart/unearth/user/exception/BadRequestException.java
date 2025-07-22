package com.rhoonart.unearth.user.exception;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(ResponseCode.BAD_REQUEST, message);
    }

    public BadRequestException(ResponseCode code, String message) {
        super(code, message);
    }

}