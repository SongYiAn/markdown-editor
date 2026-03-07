package com.songyuyang.markdowneditor.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    // 根据用户名查询用户
    Optional<UserAccount> findByUsername(String username);
}
