package com.rhoonart.unearth.common;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS("요청이 성공적으로 처리되었습니다."),
    INVALID_INPUT("입력값이 올바르지 않습니다."),
    AUTH_FAIL("아이디 또는 비밀번호가 일치하지 않습니다."),
    NOT_FOUND("요청하신 정보를 찾을 수 없습니다."),
    SERVER_ERROR("서버에 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final String message;

    ResponseCode(String message) {
        this.message = message;
    }
}