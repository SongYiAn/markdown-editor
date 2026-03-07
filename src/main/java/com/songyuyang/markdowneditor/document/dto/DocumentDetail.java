package com.songyuyang.markdowneditor.document.dto;

import com.songyuyang.markdowneditor.document.DocumentRole;
import java.time.Instant;
import lombok.Getter;

@Getter
// 文档详情返回
public class DocumentDetail {
    // 文档 ID
    private final Long id;
    // 标题
    private final String title;
    // 内容
    private final String content;
    // 当前版本号
    private final long version;
    // 当前用户角色
    private final DocumentRole role;
    // 更新时间
    private final Instant updatedAt;

    public DocumentDetail(Long id, String title, String content, long version, DocumentRole role, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.version = version;
        this.role = role;
        this.updatedAt = updatedAt;
    }
}
