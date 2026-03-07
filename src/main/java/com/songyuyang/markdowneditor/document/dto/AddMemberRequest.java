package com.songyuyang.markdowneditor.document.dto;

import com.songyuyang.markdowneditor.document.DocumentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 邀请成员请求
public class AddMemberRequest {
    // 用户名
    @NotBlank
    private String username;

    // 成员角色
    @NotNull
    private DocumentRole role;
}
