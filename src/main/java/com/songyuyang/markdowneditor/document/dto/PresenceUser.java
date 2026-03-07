package com.songyuyang.markdowneditor.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 在线用户状态（展示用）
public class PresenceUser {
    // 用户 ID
    private Long userId;
    // 显示名
    private String displayName;
    // 最近光标位置（可为空）
    private Integer cursor;
}
