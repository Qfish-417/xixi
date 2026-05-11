package com.xixi.manjuagent.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token黑名单服务 - 用于管理已注销的Token
 * 防止用户登出后Token仍被滥用
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    /**
     * 将Token加入黑名单
     *
     * @param token     要拉黑的Token
     * @param expiresAt Token过期时间戳（毫秒）
     */
    public void blacklistToken(String token, long expiresAt) {
        if (token == null || token.isEmpty()) {
            return;
        }
        blacklistedTokens.put(token, expiresAt);
        log.info("Token已加入黑名单: {}...", token.substring(0, Math.min(20, token.length())));
    }

    /**
     * 将Token加入黑名单（使用默认过期时间）
     */
    public void blacklistToken(String token) {
        blacklistToken(token, System.currentTimeMillis() + jwtExpiration);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param token 要检查的Token
     * @return true表示在黑名单中，false表示不在
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        Long expiresAt = blacklistedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }

        // 如果黑名单记录已过期，标记为待清理（不在这里remove以避免并发问题）
        if (System.currentTimeMillis() > expiresAt) {
            // 延迟清理，由定时任务统一处理
            return false;
        }

        return true;
    }

    /**
     * 获取黑名单大小（用于监控）
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * 定时清理过期的黑名单记录（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int beforeSize = blacklistedTokens.size();
        
        // 使用removeIf进行批量清理
        blacklistedTokens.entrySet().removeIf(entry -> now > entry.getValue());
        
        int afterSize = blacklistedTokens.size();
        
        if (beforeSize != afterSize) {
            log.info("已清理过期黑名单记录: {} -> {}", beforeSize, afterSize);
        }
    }

    /**
     * 主动清理指定Token
     */
    public void removeToken(String token) {
        if (token != null) {
            blacklistedTokens.remove(token);
            log.debug("Token已从黑名单移除: {}...", token.substring(0, Math.min(20, token.length())));
        }
    }
}