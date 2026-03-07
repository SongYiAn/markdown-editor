package com.songyuyang.markdowneditor.document.dto;

import java.time.Instant;
import lombok.Getter;

@Getter
// 历史版本摘要
public class VersionSummary {
    private final Long id;
    private final long versionNumber;
    private final String name;
    private final boolean autoSave;
    private final Instant createdAt;
    private final String createdBy;

    public VersionSummary(Long id, long versionNumber, String name, boolean autoSave, Instant createdAt, String createdBy) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.name = name;
        this.autoSave = autoSave;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
}
