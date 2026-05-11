package com.xixi.manjuagent.agent;

import com.xixi.manjuagent.agent.model.AgentState;
import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class XixiAgent {

    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper;

    @Value("${models.default:jimeng}")
    private String defaultModelName;

    private AgentState state = AgentState.IDLE;
    private int currentStep = 0;
    private final List<TaskResult.StepResult> stepResults = new ArrayList<>();

    public XixiAgent(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
        this.objectMapper = new ObjectMapper();
    }

    public TaskResult runXixiMode(String prompt, String style, String modelName) {
        log.info("启动嘻嘻模式: prompt={}, style={}, model={}", prompt, style, modelName);
        
        if (state != AgentState.IDLE) {
            return TaskResult.builder()
                    .success(false)
                    .message("代理正在运行中")
                    .build();
        }

        state = AgentState.RUNNING;
        currentStep = 0;
        stepResults.clear();

        try {
            AiModel model = modelFactory.getModel(modelName);
            log.info("使用模型: {}", model.getName());

            String scriptResult = generateScript(model, prompt, style);
            String[] scenePrompts = parseScriptToScenes(scriptResult);
            
            List<String> imageUrls = new ArrayList<>();
            for (String scenePrompt : scenePrompts) {
                String imageUrl = generateImage(model, scenePrompt, style);
                imageUrls.add(imageUrl);
            }
            
            String videoUrl = generateVideo(model, imageUrls);

            state = AgentState.FINISHED;

            return TaskResult.builder()
                    .success(true)
                    .message("嘻嘻模式执行完成")
                    .data(videoUrl)
                    .step(currentStep)
                    .totalSteps(3)
                    .stepResults(new ArrayList<>(stepResults))
                    .build();

        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("嘻嘻模式执行失败", e);
            return TaskResult.builder()
                    .success(false)
                    .message("执行失败: " + e.getMessage())
                    .step(currentStep)
                    .totalSteps(3)
                    .stepResults(new ArrayList<>(stepResults))
                    .build();
        }
    }

    private String generateScript(AiModel model, String prompt, String style) {
        currentStep++;
        log.info("步骤 {}: 生成脚本", currentStep);
        
        Map<String, Object> params = new HashMap<>();
        params.put("theme", prompt);
        params.put("style", style != null ? style : "日系动漫");
        params.put("episodes", 1);
        
        String result = model.generateScript(params);
        
        stepResults.add(TaskResult.StepResult.builder()
                .stepNumber(currentStep)
                .toolName("scriptGenerator")
                .result("脚本生成完成")
                .timestamp(System.currentTimeMillis())
                .build());
        
        return result;
    }

    private String[] parseScriptToScenes(String scriptJson) {
        try {
            JsonNode root = objectMapper.readTree(scriptJson);
            JsonNode scenes = root.get("scenes");
            
            if (scenes != null && scenes.isArray()) {
                List<String> prompts = new ArrayList<>();
                for (JsonNode scene : scenes) {
                    String desc = scene.has("description") ? scene.get("description").asText() : "";
                    if (!desc.isEmpty()) {
                        prompts.add(desc);
                    }
                }
                return prompts.toArray(new String[0]);
            }
        } catch (JsonProcessingException e) {
            log.warn("解析脚本失败，使用默认场景", e);
        }
        
        String basePrompt = scriptJson.length() > 20 ? scriptJson.substring(0, 20) : scriptJson;
        return new String[]{basePrompt + " - 场景1", basePrompt + " - 场景2", basePrompt + " - 场景3"};
    }

    private String generateImage(AiModel model, String scenePrompt, String style) {
        currentStep++;
        log.info("步骤 {}: 生成图片 - {}", currentStep, scenePrompt);
        
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", scenePrompt);
        params.put("style", style != null ? style : "anime");
        params.put("resolution", "1024x1024");
        
        String result = model.generateImage(params);
        
        stepResults.add(TaskResult.StepResult.builder()
                .stepNumber(currentStep)
                .toolName("textToImage")
                .result("图片生成: " + result)
                .timestamp(System.currentTimeMillis())
                .build());
        
        return result;
    }

    private String generateVideo(AiModel model, List<String> imageUrls) {
        currentStep++;
        log.info("步骤 {}: 生成视频，图片数量: {}", currentStep, imageUrls.size());
        
        Map<String, Object> params = new HashMap<>();
        params.put("imageUrls", imageUrls);
        params.put("duration", imageUrls.size() * 3);
        params.put("fps", 24);
        
        String result = model.createVideo(params);
        
        stepResults.add(TaskResult.StepResult.builder()
                .stepNumber(currentStep)
                .toolName("imageToVideo")
                .result("视频生成: " + result)
                .timestamp(System.currentTimeMillis())
                .build());
        
        return result;
    }

    public AgentState getState() {
        return state;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void reset() {
        state = AgentState.IDLE;
        currentStep = 0;
        stepResults.clear();
    }
}