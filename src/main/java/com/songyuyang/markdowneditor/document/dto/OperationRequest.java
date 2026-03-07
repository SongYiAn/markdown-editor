package com.songyuyang.markdowneditor.document.dto;

import com.songyuyang.markdowneditor.document.DocumentOp.OperationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 编辑操作请求
public class OperationRequest {
    // 操作类型
    @NotNull
    private OperationType type;

    // 起始位置
    @Min(0)
    private int position;

    // 删除长度
    @Min(0)
    private int length;

    // 插入文本
    private String text;

    // 客户端基于的版本号
    @Min(0)
    private long baseVersion;
}
