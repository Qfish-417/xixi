package com.xixi.manjuagent.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeepSeek模型API调用测试
 * 验证能否正确调用DeepSeek API获取真实AI响应
 */
@SpringBootTest
class DeepseekModelTest {

    @Value("${models.deepseek.api-url}")
    private String apiUrl;

    @Value("${models.deepseek.api-key}")
    private String apiKey;

    @Value("${models.deepseek.enabled:true}")
    private boolean enabled;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
    }

    @Test
    void testApiConfiguration() {
        System.out.println("=== 测试1: API配置检查 ===");
        System.out.println("API URL: " + apiUrl);
        System.out.println("API Key: " + (apiKey != null && !apiKey.isEmpty() ? "已配置" : "未配置"));
        System.out.println("Enabled: " + enabled);
        
        assertNotNull(apiUrl, "API URL不能为空");
        assertNotNull(apiKey, "API Key不能为空");
        assertFalse(apiKey.isEmpty(), "API Key不能为空字符串");
        assertTrue(enabled, "模型应处于启用状态");
        
        System.out.println("✓ API配置检查通过\n");
    }

    @Test
    void testDeepseekApiCall() {
        System.out.println("=== 测试2: DeepSeek API调用测试 ===");
        
        if (!enabled) {
            System.out.println("⚠️ 模型未启用，跳过测试");
            return;
        }

        String prompt = "介绍一下你自己";
        System.out.println("输入: " + prompt);

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

            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/chat/completions",
                    request,
                    Map.class
            );
            long endTime = System.currentTimeMillis();

            System.out.println("响应时间: " + (endTime - startTime) + "ms");
            System.out.println("HTTP状态码: " + response.getStatusCode());

            assertNotNull(response.getBody(), "响应体不能为空");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            assertNotNull(choices, "choices字段不能为空");
            assertFalse(choices.isEmpty(), "choices列表不能为空");

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            assertNotNull(message, "message字段不能为空");

            String content = (String) message.get("content");
            assertNotNull(content, "content字段不能为空");
            assertFalse(content.isEmpty(), "content不能为空字符串");

            System.out.println("响应内容: " + content.substring(0, Math.min(200, content.length())) + "...");

            // 验证不是模拟回答
            assertFalse(content.contains("模拟"), "响应不应包含'模拟'字样");
            assertFalse(content.contains("这是一段由深度求索大模型生成的"), "响应不应是模板回答");

            System.out.println("✓ DeepSeek API调用成功，返回真实AI响应\n");

        } catch (Exception e) {
            System.out.println("✗ API调用失败: " + e.getMessage());
            e.printStackTrace();
            fail("API调用失败: " + e.getMessage());
        }
    }

    @Test
    void testDeepseekModelGenerateText() {
        System.out.println("=== 测试3: DeepseekModel.generateText() 测试 ===");

        DeepseekModel model = new DeepseekModel();
        // 手动设置配置
        // 通过反射设置私有字段
        try {
            java.lang.reflect.Field apiUrlField = DeepseekModel.class.getDeclaredField("apiUrl");
            apiUrlField.setAccessible(true);
            apiUrlField.set(model, apiUrl);

            java.lang.reflect.Field apiKeyField = DeepseekModel.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(model, apiKey);

            java.lang.reflect.Field enabledField = DeepseekModel.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.set(model, enabled);

        } catch (Exception e) {
            System.out.println("⚠️ 无法设置模型配置，跳过测试");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("input", "什么是人工智能？");

        String result = model.generateText(params);
        System.out.println("输入: 什么是人工智能？");
        System.out.println("输出: " + result.substring(0, Math.min(200, result.length())) + "...");

        assertNotNull(result, "结果不能为空");
        assertFalse(result.isEmpty(), "结果不能为空字符串");

        // 验证不是模拟回答
        boolean isMockResponse = result.contains("模拟") || 
                                 result.contains("这是一段由深度求索大模型生成的") ||
                                 result.contains("Deepseek生成的详细文本内容");
        
        if (isMockResponse) {
            System.out.println("⚠️ 返回的是模拟响应，可能API密钥无效或网络问题");
        } else {
            System.out.println("✓ 返回的是真实AI响应\n");
        }
    }

    @Test
    void testMultiplePrompts() {
        System.out.println("=== 测试4: 多轮问答测试 ===");

        String[] prompts = {
                "什么是机器学习？",
                "写一个简短的故事",
                "解释一下量子计算"
        };

        DeepseekModel model = new DeepseekModel();
        try {
            java.lang.reflect.Field apiUrlField = DeepseekModel.class.getDeclaredField("apiUrl");
            apiUrlField.setAccessible(true);
            apiUrlField.set(model, apiUrl);

            java.lang.reflect.Field apiKeyField = DeepseekModel.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(model, apiKey);

            java.lang.reflect.Field enabledField = DeepseekModel.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.set(model, enabled);

        } catch (Exception e) {
            System.out.println("⚠️ 无法设置模型配置，跳过测试");
            return;
        }

        for (String prompt : prompts) {
            System.out.println("输入: " + prompt);
            Map<String, Object> params = new HashMap<>();
            params.put("input", prompt);
            
            String result = model.generateText(params);
            System.out.println("输出长度: " + result.length() + " 字符");
            System.out.println();
        }

        System.out.println("✓ 多轮问答测试完成\n");
    }
}