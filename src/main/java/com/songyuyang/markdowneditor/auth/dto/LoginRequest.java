package com.songyuyang.markdowneditor.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 登录请求参数
public class LoginRequest {
    // 用户名
    @NotBlank
    private String username;

    // 密码
    @NotBlank
    private String password;
}
