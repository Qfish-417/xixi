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
        System.out.println("[TRACE] ExecuteAgent 初始化完成");
    }

    public TaskResult executeStep(TaskPlan.TaskStep step) {
        String toolName = step.getToolName();
        String modelName = step.getModelName();
        java.util.Map<String, Object> parameters = step.getParameters();

        System.out.println("[TRACE-EXEC] 执行步骤: tool=" + toolName + ", model=" + modelName
                + ", params=" + parameters);

        if (!rateLimiter.tryAcquire("execute-agent")) {
            System.out.println("[TRACE-EXEC] !!! 限流拦截 !!!");
            return TaskResult.builder()
                    .success(false)
                    .message("请求过于频繁，请稍后重试")
                    .build();
        }

        try {
            return circuitBreaker.execute(modelName, () -> {
                AiModel model = modelFactory.getModel(modelName);
                System.out.println("[TRACE-EXEC] 调用模型: " + (model != null ? model.getName() : "null")
                        + ", tool=" + toolName);

                String result = switch (toolName) {
                    case "textToImage" -> model.generateImage(parameters);
                    case "imageToImage" -> model.transformImage(parameters);
                    case "imageToVideo" -> model.createVideo(parameters);
                    case "scriptGenerator" -> model.generateScript(parameters);
                    case "question", "unknown" -> model.generateText(parameters);
                    default -> throw new IllegalArgumentException("未知工具: " + toolName);
                };

                String preview = result != null
                        ? result.substring(0, Math.min(100, result.length()))
                        : "null";
                System.out.println("[TRACE-EXEC] 模型返回(前100字): " + preview);

                return TaskResult.builder()
                        .success(true)
                        .message("步骤执行成功")
                        .data(result)
                        .step(step.getStepNumber())
                        .totalSteps(step.getStepNumber())
                        .build();
            });
        } catch (CircuitBreaker.CircuitBreakerException e) {
            System.out.println("[TRACE-EXEC] !!! 熔断器拦截: " + e.getMessage());
            return TaskResult.builder()
                    .success(false)
                    .message(e.getMessage())
                    .step(step.getStepNumber())
                    .totalSteps(step.getStepNumber())
                    .build();
        } catch (Exception e) {
            System.out.println("[TRACE-EXEC] !!! 执行异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return TaskResult.builder()
                    .success(false)
                    .message("执行失败: " + e.getMessage())
                    .step(step.getStepNumber())
                    .totalSteps(step.getStepNumber())
                    .build();
        }
    }
}