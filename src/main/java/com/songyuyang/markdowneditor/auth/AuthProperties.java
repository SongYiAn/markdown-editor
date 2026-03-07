package com.songyuyang.markdowneditor.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
// 认证相关配置
public class AuthProperties {
    // token 有效期（默认 1440 分钟）
    private Duration tokenTtl = Duration.ofMinutes(1440);

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }
}
