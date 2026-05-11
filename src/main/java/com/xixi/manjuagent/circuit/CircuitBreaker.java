package com.xixi.manjuagent.circuit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
@Slf4j
public class CircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final Map<String, CircuitState> circuitStates = new ConcurrentHashMap<>();
    private final CircuitBreakerConfig config;

    public CircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
    }

    public <T> T execute(String key, Supplier<T> supplier) {
        if (!config.isEnabled()) {
            return supplier.get();
        }

        CircuitState state = circuitStates.computeIfAbsent(key, k -> new CircuitState());
        
        synchronized (state) {
            switch (state.getState()) {
                case OPEN:
                    if (System.currentTimeMillis() > state.getLastFailureTime() + config.getWaitDurationMs()) {
                        state.setState(State.HALF_OPEN);
                        log.info("断路器 {} 从 OPEN 转换为 HALF_OPEN", key);
                    } else {
                        log.warn("断路器 {} 处于 OPEN 状态，拒绝请求", key);
                        throw new CircuitBreakerException("服务暂时不可用，请稍后重试");
                    }
                    break;
                case HALF_OPEN:
                    break;
                case CLOSED:
                    break;
            }
        }

        try {
            T result = supplier.get();
            onSuccess(key);
            return result;
        } catch (Exception e) {
            onFailure(key);
            throw e;
        }
    }

    private void onSuccess(String key) {
        CircuitState state = circuitStates.get(key);
        if (state != null) {
            synchronized (state) {
                state.getSuccessCount().incrementAndGet();
                state.setFailureCount(new AtomicInteger(0));
                
                if (state.getState() == State.HALF_OPEN && 
                    state.getSuccessCount().get() >= config.getSuccessThreshold()) {
                    state.setState(State.CLOSED);
                    state.getSuccessCount().set(0);
                    log.info("断路器 {} 从 HALF_OPEN 转换为 CLOSED", key);
                }
            }
        }
    }

    private void onFailure(String key) {
        CircuitState state = circuitStates.computeIfAbsent(key, k -> new CircuitState());
        
        synchronized (state) {
            state.getFailureCount().incrementAndGet();
            state.setLastFailureTime(System.currentTimeMillis());
            state.getSuccessCount().set(0);
            
            if (state.getFailureCount().get() >= config.getFailureThreshold()) {
                state.setState(State.OPEN);
                log.warn("断路器 {} 从 CLOSED 转换为 OPEN", key);
            }
        }
    }

    public State getState(String key) {
        CircuitState state = circuitStates.get(key);
        return state != null ? state.getState() : State.CLOSED;
    }

    public void reset(String key) {
        CircuitState state = circuitStates.get(key);
        if (state != null) {
            synchronized (state) {
                state.setState(State.CLOSED);
                state.getFailureCount().set(0);
                state.getSuccessCount().set(0);
            }
        }
    }

    private static class CircuitState {
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public AtomicInteger getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(AtomicInteger failureCount) {
            this.failureCount.set(failureCount.get());
        }

        public AtomicInteger getSuccessCount() {
            return successCount;
        }

        public long getLastFailureTime() {
            return lastFailureTime;
        }

        public void setLastFailureTime(long lastFailureTime) {
            this.lastFailureTime = lastFailureTime;
        }
    }

    public static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message) {
            super(message);
        }
    }
}