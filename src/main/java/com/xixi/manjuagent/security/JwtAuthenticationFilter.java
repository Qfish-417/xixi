package com.xixi.manjuagent.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import com.xixi.manjuagent.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器 - 拦截所有请求并验证Token
 * 防止Token窃取、滥用等安全问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    // 不需要Token验证的路径（白名单）
    private static final List<String> WHITELIST_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/agent/models",
            "/api/agent/tools",
            "/api/agent/status",
            "/error",
            "/health",
            "/health/details"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        
        try {
            // 检查是否为白名单路径
            if (isWhitelisted(requestUri)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 从请求头获取Token
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // 1. 检查Token是否在黑名单中
                if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                    log.warn("Token已被注销: {}...", jwt.substring(0, Math.min(20, jwt.length())));
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已失效，请重新登录");
                    return;
                }

                // 2. 验证Token有效性（捕获JWT相关异常）
                Long userId;
                String email;
                try {
                    userId = jwtUtil.getUserIdFromToken(jwt);
                    email = jwtUtil.getEmailFromToken(jwt);
                } catch (ExpiredJwtException e) {
                    log.warn("Token已过期: {}...", jwt.substring(0, Math.min(20, jwt.length())));
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已过期，请重新登录");
                    return;
                } catch (SignatureException e) {
                    log.warn("Token签名无效: {}...", jwt.substring(0, Math.min(20, jwt.length())));
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token签名无效");
                    return;
                } catch (MalformedJwtException e) {
                    log.warn("Token格式错误: {}...", jwt.substring(0, Math.min(20, jwt.length())));
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token格式错误");
                    return;
                } catch (UnsupportedJwtException e) {
                    log.warn("不支持的Token格式: {}...", jwt.substring(0, Math.min(20, jwt.length())));
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "不支持的Token格式");
                    return;
                }

                // 3. 设置用户信息到SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                email,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // 将userId存入request，供后续拦截器使用
                request.setAttribute("userId", userId);
                
                log.debug("Token验证成功，用户ID: {}, 邮箱: {}", userId, email);

            } else {
                // 没有Token
                log.warn("请求缺少Token: {}", requestUri);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "请提供有效的Token");
                return;
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Token验证过程中发生错误: {}", requestUri, ex);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    /**
     * 从请求头中提取JWT Token
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        
        return null;
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 发送统一格式的错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\": true, \"message\": \"%s\", \"status\": %d}", message, status));
    }
}