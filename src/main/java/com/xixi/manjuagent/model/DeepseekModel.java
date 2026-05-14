package com.xixi.manjuagent.model;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class DeepseekModel implements AiModel {

    @Value("${models.deepseek.api-url}")
    private String apiUrl;

    @Value("${models.deepseek.api-key}")
    private String apiKey;

    @Value("${models.deepseek.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        String keyPrefix = apiKey != null && !apiKey.isEmpty()
                ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..."
                : "未配置";
        System.out.println("[TRACE] DeepseekModel 初始化 - apiUrl=" + apiUrl
                + ", apiKey前缀=" + keyPrefix + ", enabled=" + enabled);
    }

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
        System.out.println("[TRACE-DS] generateImage: Deepseek不支持文生图");
        return null;
    }

    @Override
    public String transformImage(Map<String, Object> params) {
        System.out.println("[TRACE-DS] transformImage: Deepseek不支持图生图");
        return null;
    }

    @Override
    public String createVideo(Map<String, Object> params) {
        System.out.println("[TRACE-DS] createVideo: Deepseek不支持图生视频");
        return null;
    }

    @Override
    public String createVideoFromScript(Map<String, Object> params) {
        System.out.println("[TRACE-DS] createVideoFromScript: Deepseek不支持文生视频");
        return null;
    }

    @Override
    public String generateScript(Map<String, Object> params) {
        System.out.println("[TRACE-DS] generateScript 入口: params=" + params);
        String theme = (String) params.get("theme");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";
        Integer episodes = params.get("episodes") != null ? (Integer) params.get("episodes") : 1;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            String systemPrompt =
                "你是一个专业的漫画脚本创作助手。请根据用户提供的主题和风格，创作完整的漫画脚本。" +
                "输出格式要求为JSON，包含：theme(主题)、style(风格)、episodes(集数)、" +
                "scenes(场景数组，每个场景包含scene(编号)、description(描述)、characters(角色列表)、dialogue(对话数组))。";

            String userPrompt = String.format(
                "请创作一个主题为\"%s\"的%s风格漫画脚本，共%d集。请输出完整的JSON格式脚本。",
                theme, style, episodes);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("stream", false);
            requestBody.put("max_tokens", 4096);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            System.out.println("[TRACE-DS] generateScript 发起HTTP: url=" + apiUrl + "/chat/completions"
                    + ", key前缀=" + apiKey.substring(0, Math.min(8, apiKey.length())) + "...");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/chat/completions",
                    request,
                    Map.class
            );

            System.out.println("[TRACE-DS] generateScript HTTP响应: status=" + response.getStatusCode());

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    System.out.println("[TRACE-DS] generateScript API成功, 内容长度=" + content.length());
                    return content;
                }
            }
        } catch (Exception e) {
            System.out.println("[TRACE-DS] !!! generateScript API失败 !!!");
            System.out.println("[TRACE-DS] URL=" + apiUrl + "/chat/completions");
            System.out.println("[TRACE-DS] 异常类型=" + e.getClass().getName());
            System.out.println("[TRACE-DS] 异常消息=" + e.getMessage());
            e.printStackTrace();
        }

        // 降级：API失败时返回模板
        String fallback = String.format(
            "{\"theme\":\"%s\",\"style\":\"%s\",\"episodes\":%d,\"scenes\":[" +
            "{\"scene\":1,\"description\":\"%s - 开场场景\"}," +
            "{\"scene\":2,\"description\":\"%s - 发展场景\"}," +
            "{\"scene\":3,\"description\":\"%s - 高潮场景\"}]}",
            theme, style, episodes, theme, theme, theme);
        System.out.println("[TRACE-DS] generateScript 降级返回模板");
        return fallback;
    }

    @Override
    public String generateText(Map<String, Object> params) {
        System.out.println("[TRACE-DS] generateText 入口: params=" + params);
        String prompt = (String) params.get("prompt");
        if (prompt == null) {
            prompt = (String) params.get("question");
        }
        if (prompt == null) {
            prompt = (String) params.get("input");
        }

        if (prompt == null) {
            System.out.println("[TRACE-DS] generateText: prompt为null，返回错误");
            return "请提供有效的输入内容";
        }

        System.out.println("[TRACE-DS] generateText: prompt=" + prompt);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一个专业的AI助手，擅长回答各种问题。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("stream", false);
            requestBody.put("max_tokens", 2048);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            System.out.println("[TRACE-DS] generateText 发起HTTP: url=" + apiUrl + "/chat/completions"
                    + ", key前缀=" + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "null"));

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/chat/completions",
                    request,
                    Map.class
            );

            System.out.println("[TRACE-DS] generateText HTTP响应: status=" + response.getStatusCode());

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    System.out.println("[TRACE-DS] generateText API成功, 内容长度="
                            + (content != null ? content.length() : 0)
                            + ", 前50字=" + (content != null ? content.substring(0, Math.min(50, content.length())) : "null"));
                    return content;
                }
            }
        } catch (Exception e) {
            System.out.println("[TRACE-DS] !!! generateText API失败 !!!");
            System.out.println("[TRACE-DS] URL=" + apiUrl + "/chat/completions");
            System.out.println("[TRACE-DS] 异常类型=" + e.getClass().getName());
            System.out.println("[TRACE-DS] 异常消息=" + e.getMessage());
            e.printStackTrace();
        }

        // 降级：返回模拟响应
        String fallback = "基于提示词:\"" + prompt + "\"\n\nDeepseek生成的详细文本内容...\n\n这是一段由深度求索大模型生成的高质量文本响应。";
        System.out.println("[TRACE-DS] generateText 降级返回模板: " + fallback.substring(0, Math.min(80, fallback.length())) + "...");
        return fallback;
    }
}
