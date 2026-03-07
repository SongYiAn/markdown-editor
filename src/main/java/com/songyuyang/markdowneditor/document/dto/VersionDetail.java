package com.songyuyang.markdowneditor.document.dto;

import java.time.Instant;
import lombok.Getter;

@Getter
// 历史版本详情（包含快照内容）
public class VersionDetail {
    private final Long id;
    private final long versionNumber;
    private final String name;
    private final String content;
    private final Instant createdAt;
    private final String createdBy;

    public VersionDetail(Long id, long versionNumber, String name, String content, Instant createdAt, String createdBy) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.name = name;
        this.content = content;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
}

