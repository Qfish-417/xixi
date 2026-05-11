package com.xixi.manjuagent.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAnalysis {
    
    public enum IntentType {
        TEXT_TO_IMAGE,
        IMAGE_TO_IMAGE,
        IMAGE_TO_VIDEO,
        SCRIPT_GENERATION,
        FULL_PRODUCTION,
        QUESTION,
        UNKNOWN
    }

    private IntentType intentType;
    private String originalInput;
    private String theme;
    private String style;
    private Integer count;
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
    private String description;
}