package com.songyuyang.markdowneditor.document.dto;

import com.songyuyang.markdowneditor.document.DocumentRole;
import lombok.Getter;

@Getter
// 文档成员摘要
public class MemberSummary {
    // 用户 ID
    private final Long userId;
    // 用户名
    private final String username;
    // 显示名
    private final String displayName;
    // 角色
    private final DocumentRole role;

    public MemberSummary(Long userId, String username, String displayName, DocumentRole role) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }
}
