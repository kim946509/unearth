package com.rhoonart.unearth.song.exception;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;

public class CannotFindSongException extends BaseException {
    public CannotFindSongException() {
        super(ResponseCode.NOT_FOUND, "요청하신 음원을 찾을 수 없습니다: ");
    }
}