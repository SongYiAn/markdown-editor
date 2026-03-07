package com.songyuyang.markdowneditor.auth;

import com.songyuyang.markdowneditor.auth.dto.AuthResponse;
import com.songyuyang.markdowneditor.auth.dto.LoginRequest;
import com.songyuyang.markdowneditor.auth.dto.RegisterRequest;
import com.songyuyang.markdowneditor.common.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 认证服务：注册、登录、token 生成与校验
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AuthService(UserAccountRepository userAccountRepository,
                       AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthProperties authProperties) {
        this.userAccountRepository = userAccountRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    // 注册新用户并返回 token
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "username already exists");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userAccountRepository.save(user);
        return createToken(user);
    }

    @Transactional
    // 登录校验并返回 token
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        return createToken(user);
    }

    @Transactional(readOnly = true)
    // 校验 token 是否有效，返回用户信息
    public UserAccount authenticate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return authTokenRepository.findWithUserByToken(token)
                .filter(authToken -> authToken.getExpiresAt().isAfter(Instant.now()))
                .map(AuthToken::getUser)
                .orElse(null);
    }

    @Transactional
    // 清理过期 token
    public void revokeExpiredTokens() {
        authTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private AuthResponse createToken(UserAccount user) {
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(generateToken());
        token.setExpiresAt(Instant.now().plus(authProperties.getTokenTtl()));
        authTokenRepository.save(token);
        return new AuthResponse(token.getToken(), user.getId(), user.getDisplayName(), token.getExpiresAt());
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
