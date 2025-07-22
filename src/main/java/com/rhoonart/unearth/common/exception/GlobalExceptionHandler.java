package com.rhoonart.unearth.common.exception;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.user.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public Object handleBaseException(BaseException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        // AJAX 요청인지 확인
        if (isAjaxRequest(request)) {
            // AJAX 요청은 JSON으로 응답
            CommonResponse<Void> response = CommonResponse.fail(ex.getCode(), ex.getMessage());
            return ResponseEntity.status(getHttpStatus(ex.getCode())).body(response);
        } else {
            // 일반 페이지 요청은 원래 페이지로 리다이렉트하면서 에러 메시지 전달
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorCode", ex.getCode().name());

            // 원래 페이지로 리다이렉트 (Referer 헤더 사용)
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                return "redirect:" + referer;
            } else {
                // Referer가 없으면 홈페이지로 리다이렉트
                return "redirect:/";
            }
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<Void> handleException(Exception ex) {
        return CommonResponse.fail(ResponseCode.SERVER_ERROR, ex.getMessage());
    }

    /**
     * AJAX 요청인지 확인하는 메서드
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getHeader("Content-Type");

        // AJAX 요청의 특징들을 확인
        return (acceptHeader != null && acceptHeader.contains("application/json")) ||
                "XMLHttpRequest".equals(xRequestedWith) ||
                (contentType != null && contentType.contains("application/json"));
    }

    /**
     * ResponseCode에 따른 HTTP 상태 코드 반환
     */
    private HttpStatus getHttpStatus(ResponseCode code) {
        return switch (code) {
            case BAD_REQUEST, INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case AUTH_FAIL -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}