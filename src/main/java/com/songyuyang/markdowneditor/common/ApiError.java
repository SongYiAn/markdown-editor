package com.songyuyang.markdowneditor.common;

import java.time.Instant;
import lombok.Getter;

@Getter
// 统一错误响应结构
public class ApiError {
    // 错误消息
    private final String message;
    // 详细信息（如校验失败字段）
    private final String details;
    // 发生时间
    private final Instant timestamp;

    public ApiError(String message, String details) {
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
    }
}
