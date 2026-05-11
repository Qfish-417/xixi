package com.xixi.manjuagent.agent;

import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ManjuAgent {

    private final ModelSelector modelSelector;

    public ManjuAgent(ModelSelector modelSelector) {
        this.modelSelector = modelSelector;
    }

    public TaskResult executeTool(String toolName, String modelName, Map<String, Object> parameters) {
        log.info("执行工具: {}, 模型: {}, 参数: {}", toolName, modelName, parameters);
        
        try {
            AiModel model = modelSelector.selectModel(toolName, modelName);
            String result = executeWithModel(model, toolName, parameters);

            return TaskResult.builder()
                    .success(true)
                    .message("工具执行成功")
                    .data(result)
                    .step(1)
                    .totalSteps(1)
                    .build();
        } catch (Exception e) {
            log.error("工具执行失败", e);
            return TaskResult.builder()
                    .success(false)
                    .message("工具执行失败: " + e.getMessage())
                    .step(1)
                    .totalSteps(1)
                    .build();
        }
    }

    private String executeWithModel(AiModel model, String toolName, Map<String, Object> parameters) {
        return switch (toolName) {
            case "textToImage" -> model.generateImage(parameters);
            case "imageToImage" -> model.transformImage(parameters);
            case "imageToVideo" -> model.createVideo(parameters);
            case "scriptGenerator" -> model.generateScript(parameters);
            default -> throw new IllegalArgumentException("未知工具: " + toolName);
        };
    }

    public Map<String, Object> describeTools() {
        Map<String, Object> tools = new HashMap<>();
        
        tools.put("textToImage", Map.of(
                "description", "文生图工具：根据文字描述生成漫画风格图片",
                "parameters", Map.of(
                        "prompt", Map.of("type", "string", "required", true, "description", "图片描述"),
                        "style", Map.of("type", "string", "required", false, "description", "绘画风格"),
                        "resolution", Map.of("type", "string", "required", false, "description", "分辨率")
                ),
                "supportedModels", List.of("jimeng", "seedream")
        ));
        
        tools.put("imageToImage", Map.of(
                "description", "图生图工具：基于原图进行风格转换或修改",
                "parameters", Map.of(
                        "imageUrl", Map.of("type", "string", "required", true, "description", "原图URL"),
                        "prompt", Map.of("type", "string", "required", true, "description", "修改描述"),
                        "strength", Map.of("type", "float", "required", false, "description", "修改强度")
                ),
                "supportedModels", List.of("jimeng", "seedream")
        ));
        
        tools.put("imageToVideo", Map.of(
                "description", "图生视频工具：将图片序列合成为视频",
                "parameters", Map.of(
                        "imageUrls", Map.of("type", "array", "required", true, "description", "图片URL列表"),
                        "duration", Map.of("type", "integer", "required", false, "description", "视频时长"),
                        "fps", Map.of("type", "integer", "required", false, "description", "帧率")
                ),
                "supportedModels", List.of("jimeng", "seedance")
        ));
        
        tools.put("scriptGenerator", Map.of(
                "description", "脚本生成工具：根据主题生成漫画脚本",
                "parameters", Map.of(
                        "theme", Map.of("type", "string", "required", true, "description", "创作主题"),
                        "style", Map.of("type", "string", "required", false, "description", "风格"),
                        "episodes", Map.of("type", "integer", "required", false, "description", "集数")
                ),
                "supportedModels", List.of("jimeng")
        ));
        
        return tools;
    }

    public Map<String, Object> describeModels(List<AiModel> models) {
        return models.stream()
                .collect(Collectors.toMap(
                        AiModel::getName,
                        model -> Map.<String, Object>of(
                                "displayName", model.getDisplayName(),
                                "description", model.getDescription(),
                                "status", model.isEnabled() ? "enabled" : "disabled",
                                "capabilities", model.getCapabilities(),
                                "default", false
                        )
                ));
    }
}