package com.xixi.manjuagent.circuit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "circuit-breaker")
public class CircuitBreakerConfig {
    private boolean enabled = true;
    private int failureThreshold = 5;
    private int successThreshold = 3;
    private long waitDurationMs = 60000;
    private long timeoutMs = 30000;
}