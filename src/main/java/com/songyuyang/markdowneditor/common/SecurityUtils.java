package com.songyuyang.markdowneditor.common;

import com.songyuyang.markdowneditor.auth.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    // 获取当前登录用户 ID（未登录则抛异常）
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return principal.getUserId();
    }
}
