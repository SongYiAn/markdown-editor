package com.songyuyang.markdowneditor.document.dto;

import com.songyuyang.markdowneditor.document.DocumentRole;
import java.time.Instant;
import lombok.Getter;

@Getter
// 文档列表项
public class DocumentSummary {
    // 文档 ID
    private final Long id;
    // 标题
    private final String title;
    // 当前用户角色
    private final DocumentRole role;
    // 更新时间
    private final Instant updatedAt;

    public DocumentSummary(Long id, String title, DocumentRole role, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.role = role;
        this.updatedAt = updatedAt;
    }
}
