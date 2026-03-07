package com.songyuyang.markdowneditor.document.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 在线状态消息体：用于 join/leave/cursor/typing 及快照广播
public class PresenceMessage {
    // 消息类型：join/leave/cursor/typing/snapshot/ping
    private String type;
    // 发送者用户 ID
    private Long userId;
    // 发送者显示名
    private String displayName;
    // 光标位置（可为空）
    private Integer cursor;
    // 可选 token，用于在 STOMP principal 缺失时鉴权
    private String token;
    // 快照时的在线用户列表
    private List<PresenceUser> users;
}
