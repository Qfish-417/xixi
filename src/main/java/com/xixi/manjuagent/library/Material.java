package com.xixi.manjuagent.library;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    private String id;
    private String name;
    private String imageUrl;
    private String description;
    private String category;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private String type;
    private String groupId;
    @Builder.Default
    private Integer usageCount = 0;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public enum Category {
        CHARACTER("character", "角色素材"),
        BACKGROUND("background", "背景素材"),
        PROP("prop", "道具素材"),
        EFFECT("effect", "特效素材"),
        SCRIPT("script", "脚本素材"),
        OTHER("other", "其他");

        private final String code;
        private final String label;

        Category(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static Category fromCode(String code) {
            for (Category category : values()) {
                if (category.code.equalsIgnoreCase(code)) {
                    return category;
                }
            }
            return OTHER;
        }
    }

    public enum Type {
        IMAGE("image"),
        VIDEO("video"),
        AUDIO("audio"),
        TEXT("text");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}