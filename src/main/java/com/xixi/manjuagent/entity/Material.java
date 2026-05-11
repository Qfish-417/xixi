package com.xixi.manjuagent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    private Long id;
    
    private Long userId;
    
    private Long groupId;
    
    private String name;
    
    private String type;
    
    private String uuid;
    
    private String originalFilename;
    
    private String url;
    
    private String thumbnailUrl;
    
    private String tags;
    
    private String description;
    
    private String originalPrompt;
    
    private String optimizedPrompt;
    
    private String modelName;
    
    private String style;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}