package com.xixi.manjuagent.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ModelSelector {

    private final ModelFactory modelFactory;

    public ModelSelector(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    public AiModel selectModel(String toolName) {
        List<AiModel> capableModels = modelFactory.getEnabledModels().stream()
                .filter(model -> model.getCapabilities().contains(toolName))
                .collect(Collectors.toList());

        if (capableModels.isEmpty()) {
            log.warn("没有模型支持工具: {}, 使用默认模型", toolName);
            return modelFactory.getDefaultModel();
        }

        AiModel defaultModel = modelFactory.getDefaultModel();
        if (defaultModel != null && capableModels.contains(defaultModel)) {
            log.info("选择默认模型 {} 执行工具 {}", defaultModel.getName(), toolName);
            return defaultModel;
        }

        AiModel selected = capableModels.get(0);
        log.info("选择模型 {} 执行工具 {}", selected.getName(), toolName);
        return selected;
    }

    public AiModel selectModel(String toolName, String preferredModelName) {
        if (preferredModelName != null && !preferredModelName.isEmpty()) {
            AiModel preferredModel = modelFactory.getModel(preferredModelName);
            if (preferredModel != null && preferredModel.getCapabilities().contains(toolName)) {
                log.info("使用用户指定的模型 {} 执行工具 {}", preferredModelName, toolName);
                return preferredModel;
            }
            log.warn("用户指定的模型 {} 不支持工具 {}，自动选择", preferredModelName, toolName);
        }

        return selectModel(toolName);
    }
}