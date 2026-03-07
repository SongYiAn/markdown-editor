package com.songyuyang.markdowneditor.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 创建文档请求参数
public class DocumentCreateRequest {
    // 标题（最大 200 字符）
    @NotBlank
    @Size(max = 200)
    private String title;

    // 初始内容（可为空）
    private String content;
}
