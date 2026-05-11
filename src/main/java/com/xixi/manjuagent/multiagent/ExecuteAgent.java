package com.xixi.manjuagent.multiagent;

import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.circuit.CircuitBreaker;
import com.xixi.manjuagent.circuit.RateLimiter;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExecuteAgent {

    private final ModelFactory modelFactory;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    public ExecuteAgent(ModelFactory modelFactory, CircuitBreaker circuitBreaker, RateLimiter rateLimiter) {
        this.modelFactory = modelFactory;
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
    }

    public TaskResult executeStep(TaskPlan.TaskStep step) {
        String toolName = step.getToolName();
        String modelName = step.getModelName();
        java.util.Map<String, Object> parameters = step.getParameters();

        log.info("执行智能体执行步骤: {}, 模型: {}", toolName, modelName);

        if (!rateLimiter.tryAcquire("execute-agent")) {
            return TaskResult.builder()
                    .success(false)
                    .message("请求过于频繁，请稍后重试")
                    .build();
        }

        try {
            return circuitBreaker.execute(modelName, () -> {
                AiModel model = modelFactory.getModel(modelName);
                
                String result = switch (toolName) {
                    case "textToImage" -> model.generateImage(parameters);
                    case "imageToImage" -> model.transformImage(parameters);
                    case "imageToVideo" -> model.createVideo(parameters);
                    case "scriptGenerator" -> model.generateScript(parameters);
                    default -> throw new IllegalArgumentException("未知工具: " + toolName);
                };

                return TaskResult.builder()
                        .success(true)
                        .message("步骤执行成功")
                        .data(result)
                        .step(step.getStepNumber())
                        .totalSteps(step.getStepNumber())
                        .build();
            });
        } catch (CircuitBreaker.CircuitBreakerException e) {
            return TaskResult.builder()
                    .success(false)
                    .message(e.getMessage())
                    .step(step.getStepNumber())
                    .totalSteps(step.getStepNumber())
                    .build();
        }
    }
}