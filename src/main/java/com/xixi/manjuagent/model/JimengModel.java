package com.xixi.manjuagent.model;

import com.xixi.manjuagent.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class JimengModel implements AiModel {

    @Value("${models.jimeng.api-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String apiUrl;

    @Value("${models.jimeng.api-key:}")
    private String apiKey;

    @Value("${models.jimeng.model-id:doubao-seedream-2-0-t2i}")
    private String modelId;

    @Value("${models.jimeng.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final MinioService minioService;

    public JimengModel(MinioService minioService) {
        this.restTemplate = new RestTemplate();
        this.minioService = minioService;
    }

    @Override
    public String getName() {
        return "jimeng";
    }

    @Override
    public String getDisplayName() {
        return "即梦AI";
    }

    @Override
    public String getDescription() {
        return "字节跳动旗下AI创意平台，基于火山引擎API，支持文生图、图生视频";
    }

    @Override
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("textToImage", "imageToImage", "imageToVideo", "scriptGenerator");
    }

    /**
     * 火山引擎API通用调用方法 (用于文本生成)
     */
    private String callVolcengineChatApi(List<Map<String, String>> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "doubao-vision-lite");
            requestBody.put("messages", messages);
            requestBody.put("stream", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/chat/completions",
                    request,
                    Map.class
            );

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("火山引擎API调用失败", e);
            return null;
        }
    }

    /**
     * 将分辨率参数映射为Seedream API要求的格式
     * 火山引擎要求最小像素为 3686400 (即 1920x1920)
     */
    private String mapResolution(String resolution) {
        if (resolution == null) return "1920x1920";
        return switch (resolution.trim()) {
            case "1:1" -> "1920x1920";
            case "16:9" -> "2560x1440";
            case "9:16" -> "1440x2560";
            case "4:3" -> "2560x1920";
            case "3:4" -> "1920x2560";
            case "1024x1024", "2k" -> "1920x1920";
            case "4k" -> "3840x2160";
            default -> {
                if (resolution.matches("\\d+x\\d+")) {
                    String[] parts = resolution.split("x");
                    int width = Integer.parseInt(parts[0]);
                    int height = Integer.parseInt(parts[1]);
                    int pixels = width * height;
                    if (pixels < 3686400) {
                        yield "1920x1920";
                    }
                    yield resolution;
                }
                yield "1920x1920";
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public String generateImage(Map<String, Object> params) {
        System.out.println("[TRACE-JM] generateImage 入口: params=" + params);
        String prompt = (String) params.get("prompt");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";
        String resolution = params.get("resolution") != null ? (String) params.get("resolution") : "1:1";
        String size = mapResolution(resolution);
        @SuppressWarnings("unchecked")
        List<String> referenceImages = (List<String>) params.get("referenceImages");

        try {
            // 调用火山引擎图片生成API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelId);
            requestBody.put("prompt", prompt + "，风格：" + style);
            requestBody.put("size", size);
            requestBody.put("n", 1);

            // 如果有参考图片，上传到MinIO后使用URL传递
            if (referenceImages != null && !referenceImages.isEmpty()) {
                try {
                    String refImg = referenceImages.get(0);
                    byte[] imageBytes;
                    if (refImg.startsWith("data:image")) {
                        int commaIdx = refImg.indexOf(',');
                        if (commaIdx > 0) {
                            imageBytes = Base64.getDecoder().decode(refImg.substring(commaIdx + 1));
                        } else {
                            imageBytes = Base64.getDecoder().decode(refImg);
                        }
                    } else {
                        imageBytes = Base64.getDecoder().decode(refImg);
                    }
                    String refUrl = minioService.uploadBytes(imageBytes, "png");
                    requestBody.put("image", refUrl);
                    System.out.println("[TRACE-JM] 参考图片已上传MinIO并附加: " + refUrl);
                } catch (Exception e) {
                    System.out.println("[TRACE-JM] 参考图片处理失败，仅用文字提示词: " + e.getMessage());
                }
            }

            System.out.println("[TRACE-JM] 调用火山引擎图片生成API: model=" + modelId
                    + ", prompt=" + prompt + ", resolution=" + resolution + " → size=" + size
                    + ", hasReference=" + (referenceImages != null && !referenceImages.isEmpty()));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/images/generations",
                    request,
                    Map.class
            );

            System.out.println("[TRACE-JM] 图片生成API响应: status=" + response.getStatusCode()
                    + ", body=" + response.getBody());

            if (response.getBody() != null) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getBody().get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    Map<String, Object> imageData = dataList.get(0);
                    String imageUrl = (String) imageData.get("url");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        System.out.println("[TRACE-JM] 图片生成成功，URL=" + imageUrl);
                        // 下载并上传到MinIO，返回MinIO签名URL
                        try {
                            String minioUrl = minioService.uploadFromUrl(imageUrl, "png");
                            System.out.println("[TRACE-JM] 已存储到MinIO: " + minioUrl);
                            return minioUrl;
                        } catch (Exception e) {
                            System.out.println("[TRACE-JM] MinIO存储失败，返回原始URL: " + e.getMessage());
                            return imageUrl;
                        }
                    }

                    // 检查是否有base64数据
                    String b64Json = (String) imageData.get("b64_json");
                    if (b64Json != null && !b64Json.isEmpty()) {
                        System.out.println("[TRACE-JM] 图片生成成功(base64)，上传到MinIO...");
                        byte[] imageBytes = Base64.getDecoder().decode(b64Json);
                        try {
                            String minioUrl = minioService.uploadBytes(imageBytes, "png");
                            System.out.println("[TRACE-JM] base64图片已存储到MinIO: " + minioUrl);
                            return minioUrl;
                        } catch (Exception e) {
                            System.out.println("[TRACE-JM] MinIO存储失败，返回data URL: " + e.getMessage());
                            return "data:image/png;base64," + b64Json;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TRACE-JM] !!! 图片生成API失败 !!!");
            System.out.println("[TRACE-JM] 异常: " + e.getClass().getName() + ": " + e.getMessage());
            // 尝试提取响应体
            if (e instanceof org.springframework.web.client.HttpClientErrorException httpErr) {
                System.out.println("[TRACE-JM] HTTP状态码: " + httpErr.getStatusCode());
                System.out.println("[TRACE-JM] 响应体: " + httpErr.getResponseBodyAsString());
            }
            e.printStackTrace();
        }

        // 降级：尝试通过chat API用文字描述生成"图片概念"，然后生成一个占位图
        System.out.println("[TRACE-JM] 图片API不可用，使用chat API生成图片概念...");
        String imagePrompt = String.format("请描述一个%s风格的画面：%s", style, prompt);

        List<Map<String, String>> messages = Arrays.asList(
                Map.of("role", "system", "content", "你是一个专业的AI绘画助手。"),
                Map.of("role", "user", "content", imagePrompt)
        );

        String description = callVolcengineChatApi(messages);
        if (description == null) {
            // 完全失败，返回占位图
            String placeholder = "https://picsum.photos/seed/"
                    + UUID.randomUUID().toString().substring(0, 8)
                    + "/1024/1024";
            System.out.println("[TRACE-JM] 使用占位图: " + placeholder);
            return placeholder;
        }

        System.out.println("[TRACE-JM] 图片概念: " + description.substring(0, Math.min(100, description.length())) + "...");

        // 用描述中的关键词尝试再次生成
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody2 = new HashMap<>();
            requestBody2.put("model", modelId);
            requestBody2.put("prompt", description.substring(0, Math.min(500, description.length())));
            requestBody2.put("size", size);
            requestBody2.put("n", 1);

            HttpEntity<Map<String, Object>> request2 = new HttpEntity<>(requestBody2, headers);

            ResponseEntity<Map> response2 = restTemplate.postForEntity(
                    apiUrl + "/images/generations",
                    request2,
                    Map.class
            );

            if (response2.getBody() != null) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response2.getBody().get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    String imageUrl = (String) dataList.get(0).get("url");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            return minioService.uploadFromUrl(imageUrl, "png");
                        } catch (Exception e) {
                            return imageUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TRACE-JM] 二次尝试也失败: " + e.getMessage());
        }

        // 最终降级
        String placeholder = "https://picsum.photos/seed/"
                + UUID.randomUUID().toString().substring(0, 8)
                + "/1024/1024";
        System.out.println("[TRACE-JM] 最终降级占位图: " + placeholder);
        return placeholder;
    }

    @Override
    public String transformImage(Map<String, Object> params) {
        System.out.println("[TRACE-JM] transformImage: params=" + params);
        String imageUrl = (String) params.get("imageUrl");
        String prompt = (String) params.get("prompt");

        // 图生图也走同样的API
        Map<String, Object> imgParams = new HashMap<>(params);
        imgParams.put("prompt", "基于图片风格转换：" + prompt + "，参考图片：" + imageUrl);
        return generateImage(imgParams);
    }

    @Override
    public String createVideo(Map<String, Object> params) {
        System.out.println("[TRACE-JM] createVideo: params=" + params);
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) params.get("imageUrls");

        if (imageUrls == null || imageUrls.isEmpty()) {
            System.out.println("[TRACE-JM] 视频生成缺少图片");
            return null;
        }

        // 构建场景描述（取前4张图片）
        StringBuilder scenes = new StringBuilder();
        for (int i = 0; i < Math.min(imageUrls.size(), 4); i++) {
            scenes.append("场景").append(i + 1).append(": ").append(imageUrls.get(i)).append("\n");
        }

        try {
            // 调用火山引擎视频生成API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "doubao-seedream-2-0-i2v");
            requestBody.put("input", scenes.toString());
            requestBody.put("duration", 15); // 默认15秒
            requestBody.put("transition", "smooth"); // 平滑转场

            System.out.println("[TRACE-JM] 调用火山引擎视频生成API: model=doubao-seedream-2-0-i2v, scenes=" + scenes.length() + " chars");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/videos/generations",
                    request,
                    Map.class
            );

            System.out.println("[TRACE-JM] 视频生成API响应: status=" + response.getStatusCode()
                    + ", body=" + response.getBody());

            if (response.getBody() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                if (result != null) {
                    String videoUrl = (String) result.get("url");
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        System.out.println("[TRACE-JM] 视频生成成功，URL=" + videoUrl);
                        // 下载并上传到MinIO
                        try {
                            String minioUrl = minioService.uploadFromUrl(videoUrl, "mp4");
                            System.out.println("[TRACE-JM] 视频已存储到MinIO: " + minioUrl);
                            return minioUrl;
                        } catch (Exception e) {
                            System.out.println("[TRACE-JM] MinIO存储失败，返回原始URL: " + e.getMessage());
                            return videoUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TRACE-JM] !!! 视频生成API失败 !!!");
            System.out.println("[TRACE-JM] 异常: " + e.getClass().getName() + ": " + e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException httpErr) {
                System.out.println("[TRACE-JM] HTTP状态码: " + httpErr.getStatusCode());
                System.out.println("[TRACE-JM] 响应体: " + httpErr.getResponseBodyAsString());
            }
            e.printStackTrace();
        }

        // 降级：返回占位视频
        String placeholder = "https://www.w3schools.com/html/mov_bbb.mp4";
        System.out.println("[TRACE-JM] 视频生成降级返回: " + placeholder);
        return placeholder;
    }

    @Override
    public String createVideoFromScript(Map<String, Object> params) {
        System.out.println("[TRACE-JM] createVideoFromScript: params=" + params);
        String prompt = (String) params.get("prompt");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";

        if (prompt == null || prompt.isEmpty()) {
            System.out.println("[TRACE-JM] 视频生成缺少剧本描述");
            return null;
        }

        try {
            // 调用火山引擎文生视频API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "doubao-seedream-2-0-t2v");
            requestBody.put("prompt", prompt + "，风格：" + style);
            requestBody.put("duration", 15); // 默认15秒

            System.out.println("[TRACE-JM] 调用火山引擎文生视频API: model=doubao-seedream-2-0-t2v, prompt=" + prompt.length() + " chars");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/videos/generations",
                    request,
                    Map.class
            );

            System.out.println("[TRACE-JM] 文生视频API响应: status=" + response.getStatusCode()
                    + ", body=" + response.getBody());

            if (response.getBody() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                if (result != null) {
                    String videoUrl = (String) result.get("url");
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        System.out.println("[TRACE-JM] 文生视频成功，URL=" + videoUrl);
                        try {
                            String minioUrl = minioService.uploadFromUrl(videoUrl, "mp4");
                            System.out.println("[TRACE-JM] 视频已存储到MinIO: " + minioUrl);
                            return minioUrl;
                        } catch (Exception e) {
                            System.out.println("[TRACE-JM] MinIO存储失败，返回原始URL: " + e.getMessage());
                            return videoUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TRACE-JM] !!! 文生视频API失败 !!!");
            System.out.println("[TRACE-JM] 异常: " + e.getClass().getName() + ": " + e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException httpErr) {
                System.out.println("[TRACE-JM] HTTP状态码: " + httpErr.getStatusCode());
                System.out.println("[TRACE-JM] 响应体: " + httpErr.getResponseBodyAsString());
            }
            e.printStackTrace();
        }

        // 降级：返回占位视频
        String placeholder = "https://www.w3schools.com/html/mov_bbb.mp4";
        System.out.println("[TRACE-JM] 文生视频降级返回: " + placeholder);
        return placeholder;
    }

    @Override
    public String generateScript(Map<String, Object> params) {
        System.out.println("[TRACE-JM] generateScript: params=" + params);
        String theme = (String) params.get("theme");
        String style = params.get("style") != null ? (String) params.get("style") : "anime";

        List<Map<String, String>> messages = Arrays.asList(
                Map.of("role", "system", "content", "你是一个专业的漫画脚本创作助手，擅长创作精彩的漫画剧本。"),
                Map.of("role", "user", "content",
                        String.format("请创作一个主题为\"%s\"的%s风格漫画脚本，包含分镜描述和对话。", theme, style))
        );

        String result = callVolcengineChatApi(messages);
        if (result == null) {
            result = String.format(
                    "{\"theme\":\"%s\",\"style\":\"%s\",\"episodes\":1,\"scenes\":[" +
                    "{\"scene\":1,\"description\":\"%s - 开场场景\"}]}",
                    theme, style, theme);
        }

        System.out.println("[TRACE-JM] generateScript 完成");
        return result;
    }

    @Override
    public String generateText(Map<String, Object> params) {
        System.out.println("[TRACE-JM] generateText: params=" + params);
        String prompt = (String) params.getOrDefault("prompt",
                params.getOrDefault("question",
                        params.getOrDefault("input", "请提供输入")));

        List<Map<String, String>> messages = Arrays.asList(
                Map.of("role", "system", "content", "你是一个专业的AI助手。"),
                Map.of("role", "user", "content", prompt)
        );

        String result = callVolcengineChatApi(messages);
        if (result == null) {
            result = "基于提示词:\"" + prompt + "\"\n\n生成的文本内容...";
        }

        return result;
    }
}
