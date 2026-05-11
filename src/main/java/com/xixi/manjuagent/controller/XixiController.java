package com.xixi.manjuagent.controller;

import com.xixi.manjuagent.agent.EnhancedXixiAgent;
import com.xixi.manjuagent.agent.EnhancedXixiAgent.XixiSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/xixi")
@RequiredArgsConstructor
public class XixiController {

    private final EnhancedXixiAgent xixiAgent;

    @PostMapping("/start")
    public ResponseEntity<?> startXixiMode(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        String style = (String) request.get("style");
        String modelName = (String) request.get("modelName");

        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入创作主题"));
        }

        try {
            XixiSession session = xixiAgent.startSession(prompt, style, modelName);
            return ResponseEntity.ok(convertSessionToResponse(session));
        } catch (Exception e) {
            log.error("启动嘻嘻模式失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "启动失败: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-script")
    public ResponseEntity<?> confirmScript(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        String response = (String) request.get("response");
        String modelName = (String) request.get("modelName");

        if (sessionId == null || response == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }

        try {
            XixiSession session = xixiAgent.confirmScript(sessionId, response, modelName);
            return ResponseEntity.ok(convertSessionToResponse(session));
        } catch (Exception e) {
            log.error("确认剧本失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-video-flow")
    public ResponseEntity<?> confirmVideoFlow(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        String response = (String) request.get("response");
        String modelName = (String) request.get("modelName");

        if (sessionId == null || response == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }

        try {
            XixiSession session = xixiAgent.confirmVideoFlow(sessionId, response, modelName);
            return ResponseEntity.ok(convertSessionToResponse(session));
        } catch (Exception e) {
            log.error("确认视频流程失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        XixiSession session = xixiAgent.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertSessionToResponse(session));
    }

    private Map<String, Object> convertSessionToResponse(XixiSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("userPrompt", session.getUserPrompt());
        response.put("style", session.getStyle());
        response.put("state", session.getState());
        response.put("waitingForUserInput", session.isWaitingForUserInput());
        response.put("waitingFor", session.getWaitingFor());
        response.put("logs", session.getLogs());
        
        if (session.getScriptPlan() != null) {
            response.put("scenes", session.getScriptPlan().getScenes());
        }
        
        if (session.getSceneOptimizations() != null) {
            response.put("sceneOptimizations", session.getSceneOptimizations());
        }
        
        if (session.getGeneratedImageUrls() != null) {
            response.put("generatedImages", session.getGeneratedImageUrls());
        }
        
        if (session.getVideoFlowPlan() != null) {
            response.put("videoFlow", session.getVideoFlowPlan());
        }
        
        if (session.getFinalVideoUrl() != null) {
            response.put("finalVideoUrl", session.getFinalVideoUrl());
        }
        
        return response;
    }
}
