package com.xixi.manjuagent.tools;

import com.xixi.manjuagent.agent.ToolCallAgent.ToolInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ImageToVideoTool implements ToolInterface {

    @Override
    public String getName() {
        return "imageToVideo";
    }

    @Override
    public String getDescription() {
        return "图生视频工具：将图片序列合成为视频";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(Map<String, Object> parameters) {
        List<String> imageUrls = (List<String>) parameters.get("imageUrls");
        Integer duration = parameters.get("duration") != null ? ((Number) parameters.get("duration")).intValue() : 10;
        Integer fps = parameters.get("fps") != null ? ((Number) parameters.get("fps")).intValue() : 24;
        
        log.info("调用图生视频工具: imageUrls数量={}, duration={}, fps={}", imageUrls.size(), duration, fps);
        
        String videoUrl = "https://example.com/generated-video-" + System.currentTimeMillis() + ".mp4";
        
        log.info("图生视频完成，结果: {}", videoUrl);
        return videoUrl;
    }
}
