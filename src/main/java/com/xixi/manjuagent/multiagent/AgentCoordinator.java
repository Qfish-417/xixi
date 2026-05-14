package com.xixi.manjuagent.multiagent;

import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.service.MaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class AgentCoordinator {

    private final ThinkAgent thinkAgent;
    private final ExecuteAgent executeAgent;
    private final MaterialService materialService;

    private final Map<String, TaskPlan> runningTasks = new ConcurrentHashMap<>();

    public AgentCoordinator(ThinkAgent thinkAgent, ExecuteAgent executeAgent, MaterialService materialService) {
        this.thinkAgent = thinkAgent;
        this.executeAgent = executeAgent;
        this.materialService = materialService;
        System.out.println("[TRACE] AgentCoordinator 初始化完成");
    }

    public TaskResult execute(String userPrompt) {
        return execute(userPrompt, null, null, null);
    }

    public TaskResult execute(String userPrompt, String modelName) {
        return execute(userPrompt, modelName, null, null);
    }

    public TaskResult execute(String userPrompt, String modelName, Long userId) {
        return execute(userPrompt, modelName, userId, null);
    }

    public TaskResult execute(String userPrompt, String modelName, Long userId, List<String> referenceImages) {
        System.out.println("[TRACE-COORD] ===== 多智能体协调器启动 =====");
        System.out.println("[TRACE-COORD] userPrompt=" + userPrompt + ", modelName=" + modelName
                + ", referenceImages=" + (referenceImages != null ? referenceImages.size() + "张" : "无"));

        // Step 1: ThinkAgent 进行AI任务规划
        System.out.println("[TRACE-COORD] → 调用 ThinkAgent.plan() ...");
        TaskPlan plan = thinkAgent.plan(userPrompt, modelName);

        // 将参考图片注入到图片/视频生成步骤的参数中
        if (referenceImages != null && !referenceImages.isEmpty()) {
            for (TaskPlan.TaskStep step : plan.getSteps()) {
                String tool = step.getToolName();
                if ("textToImage".equals(tool) || "imageToImage".equals(tool) || "imageToVideo".equals(tool)) {
                    if (step.getParameters() == null) {
                        step.setParameters(new java.util.HashMap<>());
                    }
                    step.getParameters().put("referenceImages", referenceImages);
                    System.out.println("[TRACE-COORD] 已将" + referenceImages.size() + "张参考图片注入步骤: " + tool);
                }
            }
        }
        System.out.println("[TRACE-COORD] 任务计划: taskId=" + plan.getTaskId()
                + ", 步骤数=" + plan.getSteps().size());
        for (TaskPlan.TaskStep s : plan.getSteps()) {
            System.out.println("[TRACE-COORD]   步骤" + s.getStepNumber()
                    + ": tool=" + s.getToolName()
                    + ", model=" + s.getModelName()
                    + ", params=" + s.getParameters());
        }

        runningTasks.put(plan.getTaskId(), plan);

        List<TaskResult.StepResult> stepResults = new ArrayList<>();
        String mainMessage = null;
        List<String> imageUrls = new ArrayList<>();
        String videoUrl = null;

        try {
            for (TaskPlan.TaskStep step : plan.getSteps()) {
                System.out.println("[TRACE-COORD] → 执行步骤" + step.getStepNumber()
                        + "/" + plan.getSteps().size() + ": " + step.getToolName());

                TaskResult result = executeAgent.executeStep(step);

                step.setStatus(result.isSuccess() ? "completed" : "failed");
                step.setResult(result.getData());

                if (result.isSuccess() && result.getData() != null) {
                    String toolName = step.getToolName();
                    String resultData = result.getData().toString();
                    
                    if ("textToImage".equals(toolName) || "imageToImage".equals(toolName)) {
                        imageUrls.add(resultData);
                        if (userId != null && resultData != null && !resultData.isEmpty()) {
                            try {
                                String prompt = step.getParameters() != null ? 
                                        (String) step.getParameters().get("prompt") : userPrompt;
                                String style = step.getParameters() != null ? 
                                        (String) step.getParameters().get("style") : null;
                                materialService.saveGeneratedImageUrl(userId, resultData, prompt, 
                                        step.getModelName(), style, "image");
                                System.out.println("[TRACE-COORD] 图片已保存到用户资产库: userId=" + userId);
                            } catch (Exception e) {
                                System.out.println("[TRACE-COORD] 保存图片到资产库失败: " + e.getMessage());
                            }
                        }
                    } else if ("imageToVideo".equals(toolName)) {
                        videoUrl = resultData;
                        if (userId != null && resultData != null && !resultData.isEmpty()) {
                            try {
                                materialService.saveGeneratedVideoUrl(userId, resultData, userPrompt, 
                                        step.getModelName(), null);
                                System.out.println("[TRACE-COORD] 视频已保存到用户资产库: userId=" + userId);
                            } catch (Exception e) {
                                System.out.println("[TRACE-COORD] 保存视频到资产库失败: " + e.getMessage());
                            }
                        }
                    } else {
                        mainMessage = resultData;
                    }
                }

                TaskResult.StepResult stepResult = TaskResult.StepResult.builder()
                        .stepNumber(step.getStepNumber())
                        .toolName(step.getToolName())
                        .result(result.isSuccess() ? result.getMessage() : "失败: " + result.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build();
                stepResults.add(stepResult);

                if (!result.isSuccess()) {
                    runningTasks.remove(plan.getTaskId());
                    System.out.println("[TRACE-COORD] !!! 步骤" + step.getStepNumber() + "失败，终止执行");
                    return TaskResult.builder()
                            .success(false)
                            .message("步骤 " + step.getStepNumber() + " 执行失败: " + result.getMessage())
                            .step(step.getStepNumber())
                            .totalSteps(plan.getSteps().size())
                            .stepResults(stepResults)
                            .build();
                }

                updateDependencies(plan, step);
            }

            runningTasks.remove(plan.getTaskId());

            String message = mainMessage != null ? mainMessage : "任务执行完成";
            System.out.println("[TRACE-COORD] 全部步骤完成, message前100字="
                    + message.substring(0, Math.min(100, message.length())));

            return TaskResult.builder()
                    .success(true)
                    .message(message)
                    .images(imageUrls.isEmpty() ? null : imageUrls)
                    .videoUrl(videoUrl)
                    .step(plan.getSteps().size())
                    .totalSteps(plan.getSteps().size())
                    .stepResults(stepResults)
                    .build();

        } catch (Exception e) {
            System.out.println("[TRACE-COORD] !!! 执行异常: " + e.getMessage());
            e.printStackTrace();
            runningTasks.remove(plan.getTaskId());
            return TaskResult.builder()
                    .success(false)
                    .message("任务执行失败: " + e.getMessage())
                    .stepResults(stepResults)
                    .build();
        }
    }

    private void updateDependencies(TaskPlan plan, TaskPlan.TaskStep completedStep) {
        for (TaskPlan.TaskStep step : plan.getSteps()) {
            if (step.getDependencies() != null && step.getDependencies().contains(completedStep.getStepNumber())) {
                if (completedStep.getToolName().equals("scriptGenerator") && step.getToolName().equals("textToImage")) {
                    if (step.getParameters() != null && completedStep.getResult() != null) {
                        step.getParameters().put("prompt", completedStep.getResult().toString());
                    }
                } else if (completedStep.getToolName().equals("textToImage") && step.getToolName().equals("imageToVideo")) {
                    if (step.getParameters() == null) {
                        step.setParameters(new java.util.HashMap<>());
                    }
                    step.getParameters().put("imageUrls", List.of(completedStep.getResult().toString()));
                }
            }
        }
    }

    public TaskPlan getTaskStatus(String taskId) {
        return runningTasks.get(taskId);
    }

    public void cancelTask(String taskId) {
        runningTasks.remove(taskId);
        System.out.println("[TRACE-COORD] 任务 " + taskId + " 已取消");
    }
}
