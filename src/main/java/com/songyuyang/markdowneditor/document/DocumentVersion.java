package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "document_version")
@Getter
@Setter
@NoArgsConstructor
// 文档历史版本快照
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联文档
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    // 版本号
    @Column(nullable = false)
    private long versionNumber;

    // 快照名称（用户自定义）
    @Column
    private String name;

    // 是否为自动保存（true=自动，false=手动快照）
    @Column(nullable = false)
    private boolean autoSave = false;

    // 快照内容
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    // 创建人
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccount createdBy;

    // 创建时间
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
