package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_op")
@Getter
@Setter
@NoArgsConstructor
// 文档操作记录（插入/删除）
public class DocumentOp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联文档
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    // 操作者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    // 操作基于的版本
    @Column(nullable = false)
    private long baseVersion;

    // 应用后的版本
    @Column(nullable = false)
    private long appliedVersion;

    // 操作类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationType type;

    // 操作起始位置
    @Column(nullable = false)
    private int position;

    // 删除长度
    @Column(nullable = false)
    private int length;

    // 插入文本
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String text;

    // 创建时间
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // 操作类型枚举
    public enum OperationType {
        INSERT,
        DELETE
    }
}
