package com.songyuyang.markdowneditor.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 注册请求参数
public class RegisterRequest {
    // 用户名（3-64）
    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    // 密码（6-100）
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    // 显示名（1-64）
    @NotBlank
    @Size(min = 1, max = 64)
    private String displayName;
}
