package com.xixi.manjuagent.multiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ThinkAgent {

    private final ObjectMapper objectMapper;
    private final ModelFactory modelFactory;

    public ThinkAgent(ModelFactory modelFactory) {
        this.objectMapper = new ObjectMapper();
        this.modelFactory = modelFactory;
        System.out.println("[TRACE] ThinkAgent 初始化完成");
    }

    public TaskPlan plan(String userPrompt) {
        return plan(userPrompt, null);
    }

    public TaskPlan plan(String userPrompt, String modelName) {
        System.out.println("[TRACE-THINK] ===== ThinkAgent AI任务规划 =====");
        System.out.println("[TRACE-THINK] userPrompt=" + userPrompt + ", modelName=" + modelName);

        // 直接路由：图片/视频生成请求，跳过AI规划
        if ("jimeng".equals(modelName)) {
            String lower = userPrompt.toLowerCase();
            if (lower.contains("生成图片") || lower.contains("生成视频") || lower.contains("文生图")
                    || lower.contains("图生图") || lower.contains("图生视频") || lower.contains("画画")
                    || lower.contains("生成一张") || lower.contains("生成一个")) {
                System.out.println("[TRACE-THINK] 检测到jimeng图片/视频请求，直接路由跳过AI规划");
                return buildDirectImagePlan(userPrompt, modelName);
            }
        }

        // 规划始终用 deepseek（文本推理模型），modelName 传给执行层
        String planningModel = "deepseek";

        try {
            // 调用AI模型进行任务规划
            TaskPlan aiPlan = planWithAI(userPrompt, planningModel, modelName);
            if (aiPlan != null) {
                System.out.println("[TRACE-THINK] AI规划成功, 步骤数=" + aiPlan.getSteps().size());
                return aiPlan;
            }
        } catch (Exception e) {
            System.out.println("[TRACE-THINK] AI规划异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // 降级：使用规则匹配进行规划
        System.out.println("[TRACE-THINK] AI规划失败，使用规则降级方案");
        return generateDefaultPlan(userPrompt, modelName);
    }

    private TaskPlan planWithAI(String userPrompt, String planningModel, String preferredExecModel) {
        String taskId = "task-" + System.currentTimeMillis();

        AiModel model = modelFactory.getModel(planningModel);
        if (model == null) {
            System.out.println("[TRACE-THINK] 模型 " + planningModel + " 不可用");
            return null;
        }

        System.out.println("[TRACE-THINK] 使用模型 " + model.getName() + " 进行规划...");

        // 构建规划prompt，传入首选执行模型
        String planningPrompt = buildPlanningPrompt(userPrompt, preferredExecModel);

        Map<String, Object> params = new HashMap<>();
        params.put("input", planningPrompt);

        String aiResponse = model.generateText(params);
        System.out.println("[TRACE-THINK] AI原始响应(前200字): "
                + (aiResponse != null ? aiResponse.substring(0, Math.min(200, aiResponse.length())) : "null"));

        if (aiResponse == null || aiResponse.contains("这是一段由") || aiResponse.contains("生成的详细文本内容")) {
            System.out.println("[TRACE-THINK] AI返回了模板响应，无法用于规划");
            return null;
        }

        // 解析AI返回的JSON计划
        TaskPlan plan = parseTaskPlan(taskId, userPrompt, aiResponse, preferredExecModel);
        if (plan != null) {
            return plan;
        }

        System.out.println("[TRACE-THINK] JSON解析失败，使用默认计划");
        return null;
    }

    /**
     * 图片/视频请求直接规划 — 不经过AI，确保正确路由
     */
    private TaskPlan buildDirectImagePlan(String userPrompt, String modelName) {
        String taskId = "task-" + System.currentTimeMillis();
        String lower = userPrompt.toLowerCase();

        String toolName;
        Map<String, Object> params = new HashMap<>();

        if (lower.contains("视频") || lower.contains("图生视频")) {
            toolName = "imageToVideo";
            params.put("prompt", userPrompt);
            params.put("duration", 10);
            params.put("fps", 24);
        } else if (lower.contains("风格转换") || lower.contains("图生图") || lower.contains("基于图片")) {
            toolName = "imageToImage";
            params.put("prompt", userPrompt);
            params.put("strength", 0.7);
        } else {
            toolName = "textToImage";
            // 从输入中提取关键信息
            String prompt = userPrompt
                .replace("生成图片:", "").replace("生成图片：", "")
                .replace("生成图片", "")
                .replace("生成一张", "")
                .replace("风格:", "").replace("风格：", "")
                .replace("分辨率:", "").replace("分辨率：", "")
                .trim();
            params.put("prompt", prompt.isEmpty() ? userPrompt : prompt);
            params.put("style", "写实");
            params.put("resolution", "1:1");
        }

        System.out.println("[TRACE-THINK] 直接规划: tool=" + toolName + ", model=" + modelName);

        return TaskPlan.builder()
                .taskId(taskId)
                .prompt(userPrompt)
                .steps(List.of(
                        TaskPlan.TaskStep.builder()
                                .stepNumber(1)
                                .toolName(toolName)
                                .modelName(modelName)
                                .parameters(params)
                                .dependencies(List.of())
                                .status("pending")
                                .build()
                ))
                .build();
    }

    private String buildPlanningPrompt(String userPrompt, String preferredExecModel) {
        if (preferredExecModel == null || preferredExecModel.isEmpty()) {
            preferredExecModel = "deepseek";
        }
        return "你是一个AI任务规划专家。请分析用户的输入，选择合适的工具和模型。\n\n" +
            "【重要规则】\n" +
            "1. 如果用户只是闲聊、提问、咨询、自我介绍等普通对话 -> 使用 question 工具 + deepseek 模型\n" +
            "2. 如果用户要写剧本/故事/脚本 -> 使用 scriptGenerator 工具 + deepseek 模型\n" +
            "3. 如果用户要生成图片/画画 -> 使用 textToImage 工具 + jimeng 模型\n" +
            "4. 如果用户要修改图片/转换风格 -> 使用 imageToImage 工具 + jimeng 模型\n" +
            "5. 如果用户要生成视频/动画 -> 使用 imageToVideo 工具 + jimeng 模型\n" +
            "6. 绝大多数日常对话都应该用 question 工具，不要过度使用创作类工具\n\n" +
            "可用的工具及参数：\n" +
            "- question: 普通问答/对话 (参数: question)\n" +
            "- scriptGenerator: 生成脚本 (参数: theme, style, episodes)\n" +
            "- textToImage: 文生图 (参数: prompt, style, resolution)\n" +
            "- imageToImage: 图生图 (参数: imageUrl, prompt, strength)\n" +
            "- imageToVideo: 图生视频 (参数: imageUrls, duration, fps)\n\n" +
            "模型说明：\n" +
            "- deepseek: 擅长文本、对话、问答、脚本（仅文本类工具用它）\n" +
            "- jimeng: 擅长图片生成、视频生成（图片和视频工具必须用它）\n" +
            "- qwen: 擅长文本、图片生成\n\n" +
            "重要：用户选择的模型是 " + preferredExecModel + "。对于图片和视频类工具，优先使用此模型。\n\n" +
            "输出JSON格式（只输出JSON，不要其他内容）：\n" +
            "{\n" +
            "  \"steps\": [\n" +
            "    {\n" +
            "      \"stepNumber\": 1,\n" +
            "      \"toolName\": \"question\",\n" +
            "      \"modelName\": \"deepseek\",\n" +
            "      \"parameters\": {\"question\": \"用户的问题\"},\n" +
            "      \"dependencies\": []\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "用户输入：" + userPrompt + "\n\n" +
            "执行计划JSON:";
    }

    private TaskPlan parseTaskPlan(String taskId, String userPrompt, String aiResponse, String defaultModel) {
        try {
            // 尝试从AI响应中提取JSON
            String json = aiResponse;
            int jsonStart = aiResponse.indexOf('{');
            int jsonEnd = aiResponse.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json = aiResponse.substring(jsonStart, jsonEnd + 1);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> planMap = objectMapper.readValue(json, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) planMap.get("steps");

            if (stepsList == null || stepsList.isEmpty()) {
                System.out.println("[TRACE-THINK] AI返回的步骤列表为空");
                return null;
            }

            List<TaskPlan.TaskStep> steps = new ArrayList<>();
            for (Map<String, Object> stepMap : stepsList) {
                int stepNum = getInt(stepMap, "stepNumber", steps.size() + 1);
                String toolName = getString(stepMap, "toolName", "unknown");
                String stepModel = getString(stepMap, "modelName", defaultModel);
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) stepMap.getOrDefault("parameters", new HashMap<>());
                @SuppressWarnings("unchecked")
                List<Integer> deps = (List<Integer>) stepMap.getOrDefault("dependencies", List.of());

                steps.add(TaskPlan.TaskStep.builder()
                        .stepNumber(stepNum)
                        .toolName(toolName)
                        .modelName(stepModel)
                        .parameters(params)
                        .dependencies(deps)
                        .status("pending")
                        .build());
            }

            return TaskPlan.builder()
                    .taskId(taskId)
                    .prompt(userPrompt)
                    .steps(steps)
                    .build();

        } catch (Exception e) {
            System.out.println("[TRACE-THINK] 解析AI响应失败: " + e.getMessage());
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultVal;
    }

    /**
     * 默认计划 — AI规划失败时的降级方案
     */
    private TaskPlan generateDefaultPlan(String userPrompt, String modelName) {
        String taskId = "task-" + System.currentTimeMillis();
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "deepseek";

        return TaskPlan.builder()
                .taskId(taskId)
                .prompt(userPrompt)
                .steps(List.of(
                        TaskPlan.TaskStep.builder()
                                .stepNumber(1)
                                .toolName("question")
                                .modelName(effectiveModel)
                                .parameters(Map.of("question", userPrompt))
                                .dependencies(List.of())
                                .status("pending")
                                .build()
                ))
                .build();
    }
}
