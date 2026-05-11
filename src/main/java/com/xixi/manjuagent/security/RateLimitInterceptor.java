package com.xixi.manjuagent.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求限流拦截器 - 防止暴力破解和API滥用
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /**
     * 请求计数存储结构：key为IP或用户ID，value为请求次数和时间戳
     */
    private static class RateLimitInfo {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long timestamp = System.currentTimeMillis();
    }

    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    @Value("${rate-limiter.requests-per-second:10}")
    private int requestsPerSecond;

    @Value("${rate-limiter.burst-capacity:20}")
    private int burstCapacity;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        
        // 白名单路径不限制
        if (isWhitelisted(requestUri)) {
            return true;
        }

        // 获取客户端标识符（优先使用用户ID，否则使用IP）
        String identifier = getIdentifier(request);
        
        RateLimitInfo info = requestCounts.computeIfAbsent(identifier, k -> new RateLimitInfo());
        
        long now = System.currentTimeMillis();
        
        // 使用CAS操作重置时间窗口，避免竞态条件
        resetWindowIfNeeded(info, now);
        
        int currentCount = info.count.incrementAndGet();
        
        // 检查是否超过限制
        if (currentCount > requestsPerSecond) {
            // 检查是否允许突发
            if (currentCount > burstCapacity) {
                log.warn("请求被限流: IP={}, URI={}, 计数={}, 标识符={}", 
                        getClientIp(request), requestUri, currentCount, identifier);
                
                sendRateLimitResponse(response);
                return false;
            }
            
            // 突发警告
            log.debug("突发请求警告: IP={}, URI={}, 计数={}", 
                    getClientIp(request), requestUri, currentCount);
        }
        
        return true;
    }

    /**
     * 使用CAS操作重置时间窗口
     */
    private void resetWindowIfNeeded(RateLimitInfo info, long now) {
        long timestamp = info.timestamp;
        if (now - timestamp > 1000) {
            // CAS尝试重置，如果失败说明其他线程已重置
            if (info.count.compareAndSet(info.count.get(), 0)) {
                info.timestamp = now;
            }
        }
    }

    /**
     * 获取请求标识符
     */
    private String getIdentifier(HttpServletRequest request) {
        // 优先从SecurityContext获取用户ID（JWT过滤器已设置）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long) {
            return "user:" + auth.getPrincipal();
        }
        
        // 其次尝试从request属性获取
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }
        
        // 最后使用客户端IP
        return "ip:" + getClientIp(request);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 多个代理时，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return path.startsWith("/health") || path.startsWith("/error");
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": true, \"message\": \"请求过于频繁，请稍后再试\", \"status\": 429}");
    }
}