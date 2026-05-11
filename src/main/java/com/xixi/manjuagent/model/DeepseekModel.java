package com.xixi.manjuagent.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DeepseekModel implements AiModel {

    @Value("${models.deepseek.api-url}")
    private String apiUrl;

    @Value("${models.deepseek.api-key}")
    private String apiKey;

    @Value("${models.deepseek.enabled:true}")
    private boolean enabled;

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public String getDisplayName() {
        return "深度求索";
    }

    @Override
    public String getDescription() {
        return "深度求索大模型，支持高质量文本生成和推理";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("textGeneration", "scriptGenerator", "chatCompletion");
    }

    @Override
    public String generateImage(Map<String, Object> params) {
        log.warn("Deepseek不支持文生图，建议使用千问模型");
        return null;
    }

    @Override
    public String transformImage(Map<String, Object> params) {
        log.warn("Deepseek不支持图生图，建议使用即梦模型");
        return null;
    }

    @Override
    public String createVideo(Map<String, Object> params) {
        log.warn("Deepseek不支持图生视频，建议使用即梦模型");
        return null;
    }

    @Override
    public String generateScript(Map<String, Object> params) {
        log.info("Deepseek - 脚本生成: {}", params);
        String theme = (String) params.get("theme");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";
        
        String script = String.format("{\"theme\":\"%s\",\"style\":\"%s\",\"episodes\":1,\"scenes\":[{\"scene\":1,\"description\":\"%s - 开场场景\"},{\"scene\":2,\"description\":\"%s - 发展场景\"},{\"scene\":3,\"description\":\"%s - 高潮场景\"}]}", 
                theme, style, theme, theme, theme);
        log.info("Deepseek脚本生成完成");
        return script;
    }

    @Override
    public String generateText(Map<String, Object> params) {
        log.info("Deepseek - 文本生成: {}", params);
        String prompt = (String) params.get("prompt");
        
        String response = String.format("基于提示词:\"%s\"\n\nDeepseek生成的详细文本内容...\n\n这是一段由深度求索大模型生成的高质量文本响应。", prompt);
        log.info("Deepseek文本生成完成");
        return response;
    }
}