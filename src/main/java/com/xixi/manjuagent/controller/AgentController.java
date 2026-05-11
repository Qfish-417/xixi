package com.xixi.manjuagent.controller;

import com.xixi.manjuagent.agent.ManjuAgent;
import com.xixi.manjuagent.agent.ManjuMaster;
import com.xixi.manjuagent.agent.XixiAgent;
import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agent")
@Slf4j
public class AgentController {

    private final ManjuAgent manjuAgent;
    private final XixiAgent xixiAgent;
    private final ManjuMaster manjuMaster;
    private final ModelFactory modelFactory;

    @Value("${models.default:jimeng}")
    private String defaultModelName;

    public AgentController(ManjuAgent manjuAgent, XixiAgent xixiAgent, 
                          ManjuMaster manjuMaster, ModelFactory modelFactory) {
        this.manjuAgent = manjuAgent;
        this.xixiAgent = xixiAgent;
        this.manjuMaster = manjuMaster;
        this.modelFactory = modelFactory;
    }

    /**
     * 主智能体接口 - 自动分析用户输入并执行
     */
    @PostMapping("/master")
    public ResponseEntity<TaskResult> masterProcess(@RequestBody Map<String, Object> request) {
        String input = (String) request.get("input");
        
        log.info("主智能体请求: input={}", input);
        
        TaskResult result = manjuMaster.process(input);
        return ResponseEntity.ok(result);
    }

    /**
     * 意图分析接口 - 仅分析用户输入，不执行
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeInput(@RequestBody Map<String, Object> request) {
        String input = (String) request.get("input");
        
        log.info("意图分析请求: input={}", input);
        
        ManjuMaster.IntentAnalysis analysis = manjuMaster.analyzeInput(input);
        
        Map<String, Object> response = new HashMap<>();
        response.put("intentType", analysis.getIntentType().name());
        response.put("theme", analysis.getTheme());
        response.put("style", analysis.getStyle());
        response.put("parameters", analysis.getParameters());
        response.put("description", analysis.getDescription());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<TaskResult> executeTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("toolName");
        String modelName = (String) request.get("model");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
        
        log.info("执行工具请求: toolName={}, model={}", toolName, modelName);
        
        TaskResult result = manjuAgent.executeTool(toolName, modelName, parameters);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/xixi")
    public ResponseEntity<TaskResult> runXixiMode(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        String style = (String) request.get("style");
        String modelName = (String) request.get("model");
        
        log.info("嘻嘻模式请求: prompt={}, style={}, model={}", prompt, style, modelName);
        
        TaskResult result = xixiAgent.runXixiMode(prompt, style, modelName);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> getTools() {
        Map<String, Object> response = new HashMap<>();
        response.put("tools", manjuAgent.describeTools());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getModels() {
        List<AiModel> models = modelFactory.getAllModels();
        
        List<Map<String, Object>> modelList = models.stream()
                .map(model -> Map.<String, Object>of(
                        "name", model.getName(),
                        "displayName", model.getDisplayName(),
                        "description", model.getDescription(),
                        "status", model.isEnabled() ? "enabled" : "disabled",
                        "capabilities", model.getCapabilities(),
                        "default", model.getName().equals(defaultModelName)
                ))
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("models", modelList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/models/{modelName}")
    public ResponseEntity<Map<String, Object>> getModelDetails(@PathVariable String modelName) {
        AiModel model = modelFactory.getModel(modelName);
        
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("apiUrl", "https://api.jimeng.com");
        config.put("supportedStyles", List.of("anime", "realistic", "cartoon"));
        config.put("maxResolution", "4096x4096");
        config.put("maxVideoDuration", 180);
        
        Map<String, Object> response = new HashMap<>();
        response.put("name", model.getName());
        response.put("displayName", model.getDisplayName());
        response.put("description", model.getDescription());
        response.put("status", model.isEnabled() ? "enabled" : "disabled");
        response.put("capabilities", model.getCapabilities());
        response.put("config", config);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("state", xixiAgent.getState().name());
        status.put("currentStep", xixiAgent.getCurrentStep());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        xixiAgent.reset();
        Map<String, String> response = new HashMap<>();
        response.put("message", "代理已重置");
        return ResponseEntity.ok(response);
    }
}