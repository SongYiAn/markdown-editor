package com.songyuyang.markdowneditor.common;

import org.springframework.http.HttpStatus;

// 业务异常：携带 HTTP 状态码
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // 返回 HTTP 状态码
    public HttpStatus getStatus() {
        return status;
    }
}
