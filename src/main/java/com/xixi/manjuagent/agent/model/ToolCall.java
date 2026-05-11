package com.xixi.manjuagent.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {
    private String toolName;
    private Map<String, Object> parameters;
}