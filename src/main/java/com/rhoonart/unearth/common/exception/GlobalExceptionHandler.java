package com.rhoonart.unearth.common.exception;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.user.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return CommonResponse.fail(ResponseCode.INVALID_INPUT, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return CommonResponse.fail(ResponseCode.INVALID_INPUT, message);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorizedException(UnauthorizedException ex) {
        // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
        return "redirect:/user/login";
    }

    @ExceptionHandler(BaseException.class)
    @ResponseBody
    public CommonResponse<Void> handleBaseException(BaseException ex) {
        // 모든 BaseException 계열 예외를 JSON 응답으로 처리
        return CommonResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<Void> handleException(Exception ex) {
        return CommonResponse.fail(ResponseCode.SERVER_ERROR, ex.getMessage());
    }
}