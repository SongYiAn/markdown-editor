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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"document_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
// 文档成员关系
public class DocumentMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联文档
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    // 关联用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    // 成员角色
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentRole role;

    // 加入时间
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
