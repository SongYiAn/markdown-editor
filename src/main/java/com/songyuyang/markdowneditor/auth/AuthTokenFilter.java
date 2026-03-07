package com.songyuyang.markdowneditor.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
// 从请求头解析 token，并写入 Spring Security 上下文
public class AuthTokenFilter extends OncePerRequestFilter {
    private final AuthService authService;

    public AuthTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    // 每个请求执行：解析 token 并注入用户身份
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            UserAccount user = authService.authenticate(token);
            if (user != null) {
                UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    // 从 Authorization 或 X-Auth-Token 解析 token
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String tokenHeader = request.getHeader("X-Auth-Token");
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader;
        }
        return null;
    }
}
