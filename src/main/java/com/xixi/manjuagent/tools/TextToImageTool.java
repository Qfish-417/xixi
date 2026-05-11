package com.xixi.manjuagent.tools;

import com.xixi.manjuagent.agent.ToolCallAgent.ToolInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class TextToImageTool implements ToolInterface {

    @Value("${tools.text-to-image.api-url:}")
    private String apiUrl;

    @Value("${tools.text-to-image.api-key:}")
    private String apiKey;

    @Override
    public String getName() {
        return "textToImage";
    }

    @Override
    public String getDescription() {
        return "文生图工具：根据文字描述生成漫画风格图片";
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        String prompt = (String) parameters.get("prompt");
        String style = parameters.get("style") != null ? (String) parameters.get("style") : "anime";
        String resolution = parameters.get("resolution") != null ? (String) parameters.get("resolution") : "1024x1024";
        
        log.info("调用文生图工具: prompt={}, style={}, resolution={}", prompt, style, resolution);
        
        String imageUrl = "https://example.com/generated-image-" + System.currentTimeMillis() + ".png";
        
        log.info("文生图完成，结果: {}", imageUrl);
        return imageUrl;
    }
}
