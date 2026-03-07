package com.songyuyang.markdowneditor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
// 密码加密配置
public class PasswordConfig {
    @Bean
    // BCrypt 加密器
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
