package com.songyuyang.markdowneditor.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
// 用户账户信息
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 登录用户名
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    // 密码哈希
    @Column(nullable = false, length = 200)
    private String passwordHash;

    // 展示名
    @Column(nullable = false, length = 64)
    private String displayName;

    // 创建时间
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
