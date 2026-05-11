package com.xixi.manjuagent.multiagent;

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
public class TaskPlan {
    private String taskId;
    private String prompt;
    @Builder.Default
    private List<TaskStep> steps = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStep {
        private int stepNumber;
        private String toolName;
        private String modelName;
        private java.util.Map<String, Object> parameters;
        private java.util.List<Integer> dependencies;
        private String status;
        private Object result;
    }
}