package com.greathiit.bwc.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一返回结构，越权返回 403，限流返回 429。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public org.springframework.http.ResponseEntity<R<Void>> handleBiz(BizException e) {
        HttpStatus status = switch (e.getCode()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        return org.springframework.http.ResponseEntity.status(status)
                .body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleDenied(AccessDeniedException e) {
        return R.fail(403, "越权访问，操作已记录");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValidation(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            FieldError error = ex.getBindingResult().getFieldError();
            message = error.getField() + " " + error.getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getFieldError() != null) {
            message = ex.getFieldError().getField() + " " + ex.getFieldError().getDefaultMessage();
        }
        return R.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("未处理异常", e);
        return R.fail(500, "系统繁忙，请稍后重试");
    }
}
