package com.xixi.manjuagent.controller;

import com.xixi.manjuagent.dto.AuthResponse;
import com.xixi.manjuagent.dto.LoginRequest;
import com.xixi.manjuagent.dto.RegisterRequest;
import com.xixi.manjuagent.entity.User;
import com.xixi.manjuagent.security.TokenBlacklistService;
import com.xixi.manjuagent.service.UserService;
import com.xixi.manjuagent.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 处理用户注册、登录、登出等操作
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("用户注册请求: {}", request.getEmail());
            AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("用户注册失败: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("用户登录请求: {}", request.getEmail());
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("用户登录失败: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token);
            log.info("用户退出登录，Token已加入黑名单");
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "退出登录成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            // 从SecurityContext获取认证信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "未认证"));
            }

            Long userId = (Long) authentication.getPrincipal();
            User user = userService.getUserById(userId);

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
            }

            return ResponseEntity.ok(AuthResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build());
        } catch (Exception e) {
            log.error("获取当前用户失败: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "无效的Token"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");

            // 验证旧Token
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "无效的Token"));
            }

            // 将旧Token加入黑名单
            tokenBlacklistService.blacklistToken(token);

            // 生成新Token
            Long userId = jwtUtil.getUserIdFromToken(token);
            String email = jwtUtil.getEmailFromToken(token);
            String newToken = jwtUtil.generateToken(userId, email);

            log.info("Token已刷新，用户ID: {}", userId);

            return ResponseEntity.ok(Map.of(
                    "token", newToken,
                    "tokenType", "Bearer",
                    "expiresIn", jwtUtil.getExpiration()
            ));
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Token刷新失败"));
        }
    }
}