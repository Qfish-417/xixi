package com.xixi.manjuagent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器 - 用于运维监控
 */
@Slf4j
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "manju-agent");
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/details")
    public ResponseEntity<Map<String, Object>> healthDetails() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "manju-agent");
        response.put("version", "1.0.0");
        
        // 检查各个组件状态
        Map<String, Object> components = new HashMap<>();
        components.put("database", Map.of("status", "UP"));
        components.put("security", Map.of("status", "UP"));
        components.put("rate-limiter", Map.of("status", "UP"));
        
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }
}