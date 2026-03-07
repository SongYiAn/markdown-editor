package com.songyuyang.markdowneditor.document.dto;

import com.songyuyang.markdowneditor.document.DocumentOp.OperationType;
import lombok.Getter;

@Getter
// 编辑操作返回结果
public class OperationResult {
    // 应用后的版本号
    private final long appliedVersion;
    // 操作类型
    private final OperationType type;
    // 起始位置
    private final int position;
    // 删除长度
    private final int length;
    // 插入文本
    private final String text;
    // 应用后的完整内容
    private final String content;
    // 作者 ID
    private final Long authorId;

    public OperationResult(long appliedVersion, OperationType type, int position, int length, String text,
                           String content, Long authorId) {
        this.appliedVersion = appliedVersion;
        this.type = type;
        this.position = position;
        this.length = length;
        this.text = text;
        this.content = content;
        this.authorId = authorId;
    }
}
