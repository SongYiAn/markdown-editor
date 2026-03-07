package com.songyuyang.markdowneditor.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    // 查询 token 并预加载用户
    @Query("select t from AuthToken t join fetch t.user where t.token = :token")
    Optional<AuthToken> findWithUserByToken(@Param("token") String token);

    // 仅查询 token
    Optional<AuthToken> findByToken(String token);

    // 清理过期 token
    void deleteByExpiresAtBefore(Instant now);
}
