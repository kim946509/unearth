package com.rhoonart.unearth.song.exception;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;

public class SongBulkRegisterException extends BaseException {
    public SongBulkRegisterException(ResponseCode code , String message) {
        super(code,message);
    }
}
