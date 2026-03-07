package com.songyuyang.markdowneditor.auth;

import com.songyuyang.markdowneditor.auth.dto.LoginRequest;
import com.songyuyang.markdowneditor.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
// 认证服务测试
class AuthServiceTests {

    @Autowired
    private AuthService authService;

    @Test
    // 注册后可正常登录并拿到 token
    void registerAndLogin() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("alice");
        register.setPassword("password123");
        register.setDisplayName("Alice");
        authService.register(register);

        LoginRequest login = new LoginRequest();
        login.setUsername("alice");
        login.setPassword("password123");
        assertThat(authService.login(login).getToken()).isNotBlank();
    }
}
