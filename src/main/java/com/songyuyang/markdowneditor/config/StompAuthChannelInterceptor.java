package com.songyuyang.markdowneditor.config;

import com.songyuyang.markdowneditor.auth.AuthService;
import com.songyuyang.markdowneditor.auth.UserAccount;
import com.songyuyang.markdowneditor.auth.UserPrincipal;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
// STOMP 连接鉴权：在 CONNECT 时解析 token
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final AuthService authService;

    public StompAuthChannelInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    // CONNECT 阶段校验 token 并设置用户身份
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token != null) {
                UserAccount user = authService.authenticate(token);
                if (user != null) {
                    accessor.setUser(new UserPrincipal(user.getId(), user.getUsername()));
                }
            }
        }
        return message;
    }

    // 从 STOMP 原生头解析 token
    private String resolveToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        List<String> tokenHeaders = accessor.getNativeHeader("X-Auth-Token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        return null;
    }
}
