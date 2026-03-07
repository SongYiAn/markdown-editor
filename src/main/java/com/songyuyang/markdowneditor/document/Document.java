package com.songyuyang.markdowneditor.document;

import com.songyuyang.markdowneditor.auth.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
// 文档实体：内容、版本与成员关系
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 标题
    @Column(nullable = false, length = 200)
    private String title;

    // 正文内容（Markdown）
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content = "";

    // 当前版本号
    @Column(nullable = false)
    private long version = 0;

    // 创建时间
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // 更新时间
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // 成员列表
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentMember> members = new ArrayList<>();

    // 初始化拥有者成员
    public void addOwner(UserAccount owner) {
        DocumentMember member = new DocumentMember();
        member.setDocument(this);
        member.setUser(owner);
        member.setRole(DocumentRole.OWNER);
        members.add(member);
    }
}
