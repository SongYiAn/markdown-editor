package com.songyuyang.markdowneditor.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
// 全局异常处理：统一返回 ApiError
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    // 处理业务异常
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        ApiError error = new ApiError(ex.getMessage(), null);
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 处理参数校验异常
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .findFirst()
                .orElse("validation error");
        ApiError error = new ApiError("validation failed", details);
        return ResponseEntity.badRequest().body(error);
    }
}
