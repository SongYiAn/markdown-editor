package com.songyuyang.markdowneditor.auth.dto;

import java.time.Instant;
import lombok.Getter;

@Getter
// 登录/注册返回的 token 信息
public class AuthResponse {
    // 访问 token
    private final String token;
    // 用户 ID
    private final Long userId;
    // 展示名称
    private final String displayName;
    // 过期时间
    private final Instant expiresAt;

    public AuthResponse(String token, Long userId, String displayName, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.displayName = displayName;
        this.expiresAt = expiresAt;
    }
}
