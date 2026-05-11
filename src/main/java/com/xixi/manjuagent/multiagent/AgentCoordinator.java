package com.xixi.manjuagent.multiagent;

import com.xixi.manjuagent.agent.model.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${multi-agent.execute-agent.max-parallel-tasks:5}")
    private int maxParallelTasks;

    private final Map<String, TaskPlan> runningTasks = new ConcurrentHashMap<>();

    public AgentCoordinator(ThinkAgent thinkAgent, ExecuteAgent executeAgent) {
        this.thinkAgent = thinkAgent;
        this.executeAgent = executeAgent;
    }

    public TaskResult execute(String userPrompt) {
        log.info("智能体协调器开始执行任务: {}", userPrompt);

        TaskPlan plan = thinkAgent.plan(userPrompt);
        runningTasks.put(plan.getTaskId(), plan);

        List<TaskResult.StepResult> stepResults = new ArrayList<>();

        try {
            for (TaskPlan.TaskStep step : plan.getSteps()) {
                log.info("执行步骤 {}/{}: {}", step.getStepNumber(), plan.getSteps().size(), step.getToolName());
                
                TaskResult result = executeAgent.executeStep(step);
                
                step.setStatus(result.isSuccess() ? "completed" : "failed");
                step.setResult(result.getData());

                TaskResult.StepResult stepResult = TaskResult.StepResult.builder()
                        .stepNumber(step.getStepNumber())
                        .toolName(step.getToolName())
                        .result(result.isSuccess() ? result.getMessage() : "失败: " + result.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build();
                stepResults.add(stepResult);

                if (!result.isSuccess()) {
                    runningTasks.remove(plan.getTaskId());
                    return TaskResult.builder()
                            .success(false)
                            .message("步骤 " + step.getStepNumber() + " 执行失败")
                            .step(step.getStepNumber())
                            .totalSteps(plan.getSteps().size())
                            .stepResults(stepResults)
                            .build();
                }

                updateDependencies(plan, step);
            }

            runningTasks.remove(plan.getTaskId());

            return TaskResult.builder()
                    .success(true)
                    .message("任务执行完成")
                    .step(plan.getSteps().size())
                    .totalSteps(plan.getSteps().size())
                    .stepResults(stepResults)
                    .build();

        } catch (Exception e) {
            log.error("任务执行失败", e);
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
                        step.getParameters().put("prompt", completedStep.getResult().toString() + "场景");
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
        log.info("任务 {} 已取消", taskId);
    }
}