package com.songyuyang.markdowneditor.auth;

import java.security.Principal;

// 当前登录用户身份（写入 Spring Security）
public class UserPrincipal implements Principal {
    private final Long userId;
    private final String username;

    public UserPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // 返回用户 ID
    public Long getUserId() {
        return userId;
    }

    @Override
    // Principal 接口用户名
    public String getName() {
        return username;
    }
}
