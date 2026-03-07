package com.songyuyang.markdowneditor;

import com.songyuyang.markdowneditor.auth.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties.class)
// 应用入口与配置加载
public class MarkdownEditorApplication {

    // 启动 Spring Boot
    public static void main(String[] args) {
        SpringApplication.run(MarkdownEditorApplication.class, args);
    }

}
