package com.xixi.manjuagent.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ModelFactory {

    private final Map<String, AiModel> modelMap = new HashMap<>();

    @Value("${models.default:jimeng}")
    private String defaultModelName;

    @Autowired
    public ModelFactory(List<AiModel> models) {
        for (AiModel model : models) {
            if (model.isEnabled()) {
                modelMap.put(model.getName(), model);
                log.info("注册模型: {}", model.getName());
            }
        }
    }

    public AiModel getModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = defaultModelName;
        }
        
        AiModel model = modelMap.get(modelName);
        if (model == null) {
            log.warn("模型 {} 不存在，使用默认模型 {}", modelName, defaultModelName);
            model = modelMap.get(defaultModelName);
        }
        
        return model;
    }

    public AiModel getDefaultModel() {
        return modelMap.get(defaultModelName);
    }

    public List<AiModel> getAllModels() {
        return modelMap.values().stream().collect(Collectors.toList());
    }

    public List<AiModel> getEnabledModels() {
        return modelMap.values().stream()
                .filter(AiModel::isEnabled)
                .collect(Collectors.toList());
    }

    public boolean hasModel(String modelName) {
        return modelMap.containsKey(modelName);
    }
}