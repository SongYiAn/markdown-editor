package com.songyuyang.markdowneditor.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_token")
@Getter
@Setter
@NoArgsConstructor
// 登录 token 记录
public class AuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // token 内容
    @Column(nullable = false, unique = true, length = 120)
    private String token;

    // 关联用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private UserAccount user;

    // 过期时间
    @Column(nullable = false)
    private Instant expiresAt;

    // 创建时间
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
