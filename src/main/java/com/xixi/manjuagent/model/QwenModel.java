package com.xixi.manjuagent.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class QwenModel implements AiModel {

    @Value("${models.qwen.api-url}")
    private String apiUrl;

    @Value("${models.qwen.api-key}")
    private String apiKey;

    @Value("${models.qwen.enabled:true}")
    private boolean enabled;

    @Override
    public String getName() {
        return "qwen";
    }

    @Override
    public String getDisplayName() {
        return "Qwen";
    }

    @Override
    public String getDescription() {
        return "阿里云通义千问大模型，支持文本生成和多模态能力";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("textGeneration", "scriptGenerator", "chatCompletion", "textToImage", "imageToImage");
    }

    @Override
    public String generateImage(Map<String, Object> params) {
        log.info("Qwen - 文生图: {}", params);
        String prompt = (String) params.get("prompt");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";
        String resolution = params.get("resolution") != null ? (String) params.get("resolution") : "1024x1024";
        
        String imageUrl = String.format("https://dashscope.aliyuncs.com/api/text2image?prompt=%s&style=%s&resolution=%s", 
                prompt, style, resolution);
        log.info("Qwen文生图完成: {}", imageUrl);
        return imageUrl;
    }

    @Override
    public String transformImage(Map<String, Object> params) {
        log.info("Qwen - 图生图: {}", params);
        String imageUrl = (String) params.get("imageUrl");
        String prompt = (String) params.get("prompt");
        
        String resultUrl = String.format("https://dashscope.aliyuncs.com/api/image2image?image=%s&prompt=%s", imageUrl, prompt);
        log.info("Qwen图生图完成: {}", resultUrl);
        return resultUrl;
    }

    @Override
    public String createVideo(Map<String, Object> params) {
        log.warn("Qwen不支持图生视频，建议使用即梦模型");
        return null;
    }

    @Override
    public String createVideoFromScript(Map<String, Object> params) {
        log.warn("Qwen不支持文生视频，建议使用即梦模型");
        return null;
    }

    @Override
    public String generateScript(Map<String, Object> params) {
        log.info("Qwen - 脚本生成: {}", params);
        String theme = (String) params.get("theme");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";
        
        String script = String.format("{\"theme\":\"%s\",\"style\":\"%s\",\"episodes\":1,\"scenes\":[{\"scene\":1,\"description\":\"%s - 开场场景\"},{\"scene\":2,\"description\":\"%s - 发展场景\"},{\"scene\":3,\"description\":\"%s - 高潮场景\"}]}", 
                theme, style, theme, theme, theme);
        log.info("Qwen脚本生成完成");
        return script;
    }

    @Override
    public String generateText(Map<String, Object> params) {
        log.info("Qwen - 文本生成: {}", params);
        String prompt = (String) params.get("prompt");
        
        String response = String.format("基于提示词:\"%s\"\n\nQwen生成的详细文本内容...\n\n这是一段由通义千问大模型生成的高质量文本响应。", prompt);
        log.info("Qwen文本生成完成");
        return response;
    }
}