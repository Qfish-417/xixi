package com.xixi.manjuagent.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    private boolean success;
    private String message;
    private Object data;
    private int step;
    private int totalSteps;
    @Builder.Default
    private List<StepResult> stepResults = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        private int stepNumber;
        private String toolName;
        private String result;
        private long timestamp;
    }
}