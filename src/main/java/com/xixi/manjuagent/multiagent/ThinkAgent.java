package com.xixi.manjuagent.multiagent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ThinkAgent {

    private final ObjectMapper objectMapper;

    @Value("${multi-agent.think-agent.system-prompt:你是一个智能任务规划助手，负责分析用户需求并生成详细的执行计划。}")
    private String systemPrompt;

    public ThinkAgent() {
        this.objectMapper = new ObjectMapper();
    }

    public TaskPlan plan(String userPrompt) {
        log.info("思考智能体开始规划任务: {}", userPrompt);

        try {
            TaskPlan plan = analyzeAndPlan(userPrompt);
            log.info("思考智能体生成的计划: {}", plan.getTaskId());
            return plan;
        } catch (Exception e) {
            log.error("生成任务计划失败", e);
            return generateDefaultPlan(userPrompt);
        }
    }

    private TaskPlan analyzeAndPlan(String userPrompt) {
        String taskId = "task-" + System.currentTimeMillis();
        
        String lowerPrompt = userPrompt.toLowerCase();
        
        if (lowerPrompt.contains("生成图片") || lowerPrompt.contains("文生图")) {
            return TaskPlan.builder()
                    .taskId(taskId)
                    .prompt(userPrompt)
                    .steps(List.of(
                            TaskPlan.TaskStep.builder()
                                    .stepNumber(1)
                                    .toolName("textToImage")
                                    .modelName("jimeng")
                                    .parameters(Map.of("prompt", userPrompt, "style", "anime", "resolution", "1024x1024"))
                                    .dependencies(List.of())
                                    .status("pending")
                                    .build()
                    ))
                    .build();
        }
        
        if (lowerPrompt.contains("修改图片") || lowerPrompt.contains("图生图")) {
            return TaskPlan.builder()
                    .taskId(taskId)
                    .prompt(userPrompt)
                    .steps(List.of(
                            TaskPlan.TaskStep.builder()
                                    .stepNumber(1)
                                    .toolName("imageToImage")
                                    .modelName("jimeng")
                                    .parameters(Map.of("imageUrl", "", "prompt", userPrompt, "strength", 0.7))
                                    .dependencies(List.of())
                                    .status("pending")
                                    .build()
                    ))
                    .build();
        }
        
        if (lowerPrompt.contains("视频") || lowerPrompt.contains("动画")) {
            if (lowerPrompt.contains("图片")) {
                return TaskPlan.builder()
                        .taskId(taskId)
                        .prompt(userPrompt)
                        .steps(List.of(
                                TaskPlan.TaskStep.builder()
                                        .stepNumber(1)
                                        .toolName("textToImage")
                                        .modelName("jimeng")
                                        .parameters(Map.of("prompt", userPrompt + "场景", "style", "anime"))
                                        .dependencies(List.of())
                                        .status("pending")
                                        .build(),
                                TaskPlan.TaskStep.builder()
                                        .stepNumber(2)
                                        .toolName("imageToVideo")
                                        .modelName("jimeng")
                                        .parameters(Map.of("duration", 10, "fps", 24))
                                        .dependencies(List.of(1))
                                        .status("pending")
                                        .build()
                        ))
                        .build();
            }
            return TaskPlan.builder()
                    .taskId(taskId)
                    .prompt(userPrompt)
                    .steps(List.of(
                            TaskPlan.TaskStep.builder()
                                    .stepNumber(1)
                                    .toolName("imageToVideo")
                                    .modelName("jimeng")
                                    .parameters(Map.of("duration", 10, "fps", 24))
                                    .dependencies(List.of())
                                    .status("pending")
                                    .build()
                    ))
                    .build();
        }
        
        if (lowerPrompt.contains("脚本") || lowerPrompt.contains("故事") || lowerPrompt.contains("剧本")) {
            return TaskPlan.builder()
                    .taskId(taskId)
                    .prompt(userPrompt)
                    .steps(List.of(
                            TaskPlan.TaskStep.builder()
                                    .stepNumber(1)
                                    .toolName("scriptGenerator")
                                    .modelName("jimeng")
                                    .parameters(Map.of("theme", userPrompt, "style", "日系动漫", "episodes", 1))
                                    .dependencies(List.of())
                                    .status("pending")
                                    .build()
                    ))
                    .build();
        }
        
        return generateDefaultPlan(userPrompt);
    }

    private TaskPlan generateDefaultPlan(String userPrompt) {
        String taskId = "task-" + System.currentTimeMillis();
        
        return TaskPlan.builder()
                .taskId(taskId)
                .prompt(userPrompt)
                .steps(List.of(
                        TaskPlan.TaskStep.builder()
                                .stepNumber(1)
                                .toolName("scriptGenerator")
                                .modelName("jimeng")
                                .parameters(Map.of("theme", userPrompt, "style", "日系动漫", "episodes", 1))
                                .dependencies(List.of())
                                .status("pending")
                                .build(),
                        TaskPlan.TaskStep.builder()
                                .stepNumber(2)
                                .toolName("textToImage")
                                .modelName("jimeng")
                                .parameters(Map.of("prompt", userPrompt + "场景", "style", "anime"))
                                .dependencies(List.of(1))
                                .status("pending")
                                .build(),
                        TaskPlan.TaskStep.builder()
                                .stepNumber(3)
                                .toolName("imageToVideo")
                                .modelName("jimeng")
                                .parameters(Map.of("duration", 10))
                                .dependencies(List.of(2))
                                .status("pending")
                                .build()
                ))
                .build();
    }
}
