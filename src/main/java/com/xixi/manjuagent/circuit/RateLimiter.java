package com.xixi.manjuagent.circuit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimiter {

    private boolean enabled = true;
    private int requestsPerSecond = 10;
    private int burstCapacity = 20;

    private final Map<String, RateLimitState> rateLimitStates = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        if (!enabled) {
            return true;
        }

        RateLimitState state = rateLimitStates.computeIfAbsent(key, k -> new RateLimitState());
        return state.tryAcquire(requestsPerSecond, burstCapacity);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRequestsPerSecond(int requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public void setBurstCapacity(int burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    private static class RateLimitState {
        private final AtomicLong tokens = new AtomicLong(0);
        private volatile long lastRefillTime = System.currentTimeMillis();

        public boolean tryAcquire(int rate, int capacity) {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            
            if (timePassed >= 1000) {
                synchronized (this) {
                    if (now - lastRefillTime >= 1000) {
                        long newTokens = Math.min(capacity, tokens.get() + rate);
                        tokens.set(newTokens);
                        lastRefillTime = now;
                    }
                }
            }

            return tokens.getAndUpdate(t -> Math.max(0, t - 1)) >= 1;
        }
    }
}