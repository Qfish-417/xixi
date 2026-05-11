package com.xixi.manjuagent.agent;

import cn.hutool.core.util.StrUtil;
import com.xixi.manjuagent.agent.model.AgentState;
import com.xixi.manjuagent.agent.model.TaskResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {

    private String name;
    private String systemPrompt;
    private String nextStepPrompt;
    private AgentState state = AgentState.IDLE;
    private int currentStep = 0;
    private int maxSteps = 10;
    private List<Message> messageList = new ArrayList<>();
    private List<TaskResult.StepResult> stepResults = new ArrayList<>();

    public TaskResult run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            return TaskResult.builder()
                    .success(false)
                    .message("Cannot run agent from state: " + this.state)
                    .build();
        }
        if (StrUtil.isBlank(userPrompt)) {
            return TaskResult.builder()
                    .success(false)
                    .message("Cannot run agent with empty user prompt")
                    .build();
        }

        this.state = AgentState.RUNNING;
        messageList.add(new Message("user", userPrompt));
        stepResults.clear();

        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                currentStep = i + 1;
                log.info("Executing step {}/{}", currentStep, maxSteps);

                String stepResult = step();
                
                TaskResult.StepResult result = TaskResult.StepResult.builder()
                        .stepNumber(currentStep)
                        .toolName("step")
                        .result(stepResult)
                        .timestamp(System.currentTimeMillis())
                        .build();
                stepResults.add(result);

                if (state == AgentState.FINISHED) {
                    break;
                }
            }

            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                stepResults.add(TaskResult.StepResult.builder()
                        .stepNumber(currentStep)
                        .toolName("system")
                        .result("Terminated: Reached max steps (" + maxSteps + ")")
                        .timestamp(System.currentTimeMillis())
                        .build());
            }

            return TaskResult.builder()
                    .success(true)
                    .message("Execution completed")
                    .step(currentStep)
                    .totalSteps(maxSteps)
                    .stepResults(stepResults)
                    .build();

        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return TaskResult.builder()
                    .success(false)
                    .message("Execution error: " + e.getMessage())
                    .step(currentStep)
                    .totalSteps(maxSteps)
                    .stepResults(stepResults)
                    .build();
        } finally {
            cleanup();
        }
    }

    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }

            this.state = AgentState.RUNNING;
            messageList.add(new Message("user", userPrompt));
            stepResults.clear();

            try {
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    currentStep = i + 1;
                    log.info("Executing step {}/{}", currentStep, maxSteps);

                    String stepResult = step();

                    TaskResult.StepResult result = TaskResult.StepResult.builder()
                            .stepNumber(currentStep)
                            .toolName("step")
                            .result(stepResult)
                            .timestamp(System.currentTimeMillis())
                            .build();
                    stepResults.add(result);

                    sseEmitter.send(TaskResult.builder()
                            .success(true)
                            .message(stepResult)
                            .step(currentStep)
                            .totalSteps(maxSteps)
                            .stepResults(List.of(result))
                            .build());

                    if (state == AgentState.FINISHED) {
                        break;
                    }
                }

                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    String msg = "执行结束：达到最大步骤（" + maxSteps + "）";
                    sseEmitter.send(msg);
                }
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                cleanup();
            }
        });

        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });

        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return sseEmitter;
    }

    public abstract String step();

    public boolean think() {
        return false;
    }

    public String act() {
        return "";
    }

    protected void cleanup() {
    }

    public void pause() {
        if (state == AgentState.RUNNING) {
            state = AgentState.PAUSED;
        }
    }

    public void resume() {
        if (state == AgentState.PAUSED) {
            state = AgentState.RUNNING;
        }
    }

    public void reset() {
        state = AgentState.IDLE;
        currentStep = 0;
        messageList.clear();
        stepResults.clear();
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
