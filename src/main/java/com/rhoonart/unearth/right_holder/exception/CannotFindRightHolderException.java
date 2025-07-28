package com.rhoonart.unearth.right_holder.exception;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;

public class CannotFindRightHolderException extends BaseException {
    public CannotFindRightHolderException() {
        super(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다.");
    }
}
