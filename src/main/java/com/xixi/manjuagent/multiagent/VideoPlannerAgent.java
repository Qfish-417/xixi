package com.xixi.manjuagent.multiagent;

import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoPlannerAgent {

    private final ModelFactory modelFactory;

    @Data
    public static class VideoPlan {
        private String script;
        private List<Scene> scenes;
        private List<VideoSegment> videoSegments;
        private String transitionStyle;
        private String musicSuggestion;
        private String duration;
        private boolean success;
        private String message;
    }

    @Data
    public static class Scene {
        private int sceneNumber;
        private String description;
        private String imageUrl;
        private String prompt;
        private int duration;
        private String transition;
    }

    @Data
    public static class VideoSegment {
        private int order;
        private String type;
        private String description;
        private int duration;
        private String effect;
    }

    private static final String SYSTEM_PROMPT_SCRIPT = """
        你是一个专业的漫画/动画剧本创作专家。请根据用户的主题创作一个分镜剧本。
        
        剧本格式要求：
        场景1：[场景描述，包含画面内容、人物动作、环境氛围]
        场景2：[场景描述]
        ...
        
        每个场景需要包含：
        - 画面主体（人物/物体）
        - 动作/表情
        - 背景环境
        - 光影氛围
        - 情绪基调
        
        请生成5-8个场景，适合制作成短视频。
        """;

    private static final String SYSTEM_PROMPT_VIDEO_FLOW = """
        你是一个专业的视频制作专家。请根据提供的场景图片，设计完整的视频制作流程。
        
        需要考虑：
        1. 视频分段：每个场景如何转换为视频片段
        2. 转场效果：场景之间的过渡方式（淡入淡出、滑动、缩放等）
        3. 时长分配：每个片段的持续时间
        4. 特效建议：镜头运动、滤镜效果
        5. 音乐/音效：背景音乐风格和音效建议
        6. 字幕/文字：是否需要添加字幕
        
        输出格式：
        视频总时长：XX秒
        
        片段1：
        - 对应场景：场景X
        - 时长：X秒
        - 转场效果：[效果描述]
        - 特效：[特效描述]
        
        背景音乐建议：[风格描述]
        整体节奏：[节奏描述]
        """;

    public VideoPlan generateScript(String theme, String style, String modelName) {
        log.info("开始生成剧本: theme={}, style={}", theme, style);
        
        VideoPlan plan = new VideoPlan();
        AiModel model = modelFactory.getModel(modelName);
        
        try {
            // 生成剧本
            String fullPrompt = SYSTEM_PROMPT_SCRIPT + "\n\n主题：" + theme + "\n风格：" + style + "\n\n请创作剧本：";
            Map<String, Object> params = Map.of(
                    "prompt", fullPrompt,
                    "maxTokens", 1000,
                    "temperature", 0.8
            );
            
            String script = model.generateText(params);
            plan.setScript(script);
            
            // 解析场景
            List<Scene> scenes = parseScenes(script);
            plan.setScenes(scenes);
            
            plan.setSuccess(true);
            plan.setMessage("剧本生成成功，共 " + scenes.size() + " 个场景");
            
            log.info("剧本生成完成，共 {} 个场景", scenes.size());
            return plan;
            
        } catch (Exception e) {
            log.error("剧本生成失败", e);
            plan.setSuccess(false);
            plan.setMessage("剧本生成失败: " + e.getMessage());
            return plan;
        }
    }

    public VideoPlan planVideoFlow(List<String> imageUrls, VideoPlan scriptPlan, String modelName) {
        log.info("开始规划视频流程，图片数量: {}", imageUrls.size());
        
        VideoPlan plan = new VideoPlan();
        plan.setScenes(scriptPlan.getScenes());
        AiModel model = modelFactory.getModel(modelName);
        
        try {
            // 构建场景描述
            StringBuilder sceneDesc = new StringBuilder();
            for (int i = 0; i < scriptPlan.getScenes().size() && i < imageUrls.size(); i++) {
                Scene scene = scriptPlan.getScenes().get(i);
                scene.setImageUrl(imageUrls.get(i));
                sceneDesc.append("场景").append(i + 1).append("：").append(scene.getDescription()).append("\n");
            }
            
            String fullPrompt = SYSTEM_PROMPT_VIDEO_FLOW + "\n\n场景描述：\n" + sceneDesc + "\n\n请设计视频流程：";
            Map<String, Object> params = Map.of(
                    "prompt", fullPrompt,
                    "maxTokens", 1200,
                    "temperature", 0.7
            );
            
            String videoFlow = model.generateText(params);
            
            // 解析视频片段
            List<VideoSegment> segments = parseVideoSegments(videoFlow);
            plan.setVideoSegments(segments);
            plan.setTransitionStyle(extractTransitionStyle(videoFlow));
            plan.setMusicSuggestion(extractMusicSuggestion(videoFlow));
            plan.setDuration(extractDuration(videoFlow));
            
            plan.setSuccess(true);
            plan.setMessage("视频流程规划完成，共 " + segments.size() + " 个片段");
            
            log.info("视频流程规划完成");
            return plan;
            
        } catch (Exception e) {
            log.error("视频流程规划失败", e);
            plan.setSuccess(false);
            plan.setMessage("视频流程规划失败: " + e.getMessage());
            return plan;
        }
    }

    private List<Scene> parseScenes(String script) {
        List<Scene> scenes = new ArrayList<>();
        String[] lines = script.split("\n");
        
        int sceneNumber = 0;
        StringBuilder currentScene = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("场景") || line.matches("^\\d+[.．、]")) {
                if (currentScene.length() > 0 && sceneNumber > 0) {
                    Scene scene = new Scene();
                    scene.setSceneNumber(sceneNumber);
                    scene.setDescription(currentScene.toString().trim());
                    scene.setDuration(3); // 默认3秒
                    scene.setTransition("fade");
                    scenes.add(scene);
                }
                sceneNumber++;
                currentScene = new StringBuilder();
                
                // 提取场景描述内容
                int colonIndex = line.indexOf("：");
                if (colonIndex > 0) {
                    currentScene.append(line.substring(colonIndex + 1));
                }
            } else if (sceneNumber > 0 && !line.isEmpty()) {
                currentScene.append(" ").append(line);
            }
        }
        
        // 添加最后一个场景
        if (currentScene.length() > 0 && sceneNumber > 0) {
            Scene scene = new Scene();
            scene.setSceneNumber(sceneNumber);
            scene.setDescription(currentScene.toString().trim());
            scene.setDuration(3);
            scene.setTransition("fade");
            scenes.add(scene);
        }
        
        return scenes;
    }

    private List<VideoSegment> parseVideoSegments(String videoFlow) {
        List<VideoSegment> segments = new ArrayList<>();
        String[] lines = videoFlow.split("\n");
        
        VideoSegment currentSegment = null;
        int segmentOrder = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("片段") || line.matches("^片段\\d+")) {
                if (currentSegment != null) {
                    segments.add(currentSegment);
                }
                segmentOrder++;
                currentSegment = new VideoSegment();
                currentSegment.setOrder(segmentOrder);
                currentSegment.setType("scene");
            } else if (currentSegment != null) {
                if (line.contains("时长")) {
                    String durationStr = line.replaceAll("[^0-9]", "");
                    if (!durationStr.isEmpty()) {
                        currentSegment.setDuration(Integer.parseInt(durationStr));
                    }
                } else if (line.contains("转场") || line.contains("特效")) {
                    currentSegment.setEffect(line.substring(line.indexOf("：") + 1).trim());
                } else if (!line.isEmpty() && !line.startsWith("-")) {
                    currentSegment.setDescription(line);
                }
            }
        }
        
        if (currentSegment != null) {
            segments.add(currentSegment);
        }
        
        return segments;
    }

    private String extractTransitionStyle(String text) {
        if (text.contains("淡入淡出") || text.contains("fade")) {
            return "fade";
        } else if (text.contains("滑动") || text.contains("slide")) {
            return "slide";
        } else if (text.contains("缩放") || text.contains("zoom")) {
            return "zoom";
        }
        return "fade";
    }

    private String extractMusicSuggestion(String text) {
        int index = text.indexOf("背景音乐");
        if (index >= 0) {
            int endIndex = text.indexOf("\n", index);
            if (endIndex > index) {
                return text.substring(index, endIndex).trim();
            }
        }
        return "轻音乐";
    }

    private String extractDuration(String text) {
        int index = text.indexOf("总时长");
        if (index >= 0) {
            int endIndex = text.indexOf("\n", index);
            if (endIndex > index) {
                return text.substring(index, endIndex).trim();
            }
        }
        return "15-20秒";
    }
}
