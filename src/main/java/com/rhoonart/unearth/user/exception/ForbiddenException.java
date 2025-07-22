package com.rhoonart.unearth.user.exception;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(ResponseCode.FORBIDDEN, message);
    }

    public ForbiddenException() {
        super(ResponseCode.FORBIDDEN, "접근 권한이 없습니다.");
    }
}