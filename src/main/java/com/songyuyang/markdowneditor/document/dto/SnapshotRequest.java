package com.songyuyang.markdowneditor.document.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 保存快照请求（包含自定义名称）
public class SnapshotRequest {
    // 快照名称
    private String name;
}

