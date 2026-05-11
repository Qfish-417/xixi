package com.xixi.manjuagent.service;

import com.xixi.manjuagent.dto.AuthResponse;
import com.xixi.manjuagent.dto.LoginRequest;
import com.xixi.manjuagent.dto.RegisterRequest;
import com.xixi.manjuagent.entity.User;
import com.xixi.manjuagent.mapper.UserMapper;
import com.xixi.manjuagent.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查邮箱是否已存在
        if (userMapper.countByEmail(request.getEmail()) > 0) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 检查密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setAvatar("");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        log.info("用户注册成功: {}", user.getEmail());

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        log.info("用户登录成功: {}", user.getEmail());

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }

    public User getUserById(Long id) {
        return userMapper.findById(id);
    }

    public User getUserByEmail(String email) {
        return userMapper.findByEmail(email);
    }
}
