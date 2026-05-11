package com.xixi.manjuagent.model;

import lombok.Data;

import java.util.List;

@Data
public class ModelConfig {
    private String name;
    private String displayName;
    private String description;
    private boolean enabled;
    private String apiUrl;
    private String apiKey;
    private List<String> capabilities;
}