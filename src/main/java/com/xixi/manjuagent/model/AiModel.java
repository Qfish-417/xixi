package com.xixi.manjuagent.model;

import java.util.List;
import java.util.Map;

public interface AiModel {

    String getName();

    String getDisplayName();

    String getDescription();

    boolean isEnabled();

    List<String> getCapabilities();

    String generateImage(Map<String, Object> params);

    String transformImage(Map<String, Object> params);

    String createVideo(Map<String, Object> params);

    String generateScript(Map<String, Object> params);

    String generateText(Map<String, Object> params);
}