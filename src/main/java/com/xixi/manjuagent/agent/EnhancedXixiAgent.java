package com.xixi.manjuagent.agent;

import com.xixi.manjuagent.agent.model.AgentState;
import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.multiagent.PromptOptimizerAgent;
import com.xixi.manjuagent.multiagent.PromptOptimizerAgent.OptimizationResult;
import com.xixi.manjuagent.multiagent.VideoPlannerAgent;
import com.xixi.manjuagent.multiagent.VideoPlannerAgent.Scene;
import com.xixi.manjuagent.multiagent.VideoPlannerAgent.VideoPlan;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedXixiAgent {

    private final ModelFactory modelFactory;
    private final PromptOptimizerAgent promptOptimizer;
    private final VideoPlannerAgent videoPlanner;

    @Data
    public static class XixiSession {
        private String sessionId;
        private String userPrompt;
        private String style;
        private AgentState state;
        private VideoPlan scriptPlan;
        private List<SceneOptimization> sceneOptimizations;
        private List<String> generatedImageUrls;
        private VideoPlan videoFlowPlan;
        private String finalVideoUrl;
        private List<ProcessLog> logs;
        private boolean waitingForUserInput;
        private String waitingFor;
    }

    @Data
    public static class SceneOptimization {
        private int sceneNumber;
        private String originalDescription;
        private OptimizationResult optimizationResult;
        private String generatedImageUrl;
        private boolean completed;
    }

    @Data
    public static class ProcessLog {
        private long timestamp;
        private String stage;
        private String message;
        private Object data;
        private boolean collapsible;
    }

    private final Map<String, XixiSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public XixiSession startSession(String prompt, String style, String modelName) {
        String sessionId = "xixi-" + System.currentTimeMillis();
        log.info("启动嘻嘻模式会话: sessionId={}, prompt={}", sessionId, prompt);

        XixiSession session = new XixiSession();
        session.setSessionId(sessionId);
        session.setUserPrompt(prompt);
        session.setStyle(style != null ? style : "日系动漫");
        session.setState(AgentState.RUNNING);
        session.setSceneOptimizations(new ArrayList<>());
        session.setGeneratedImageUrls(new ArrayList<>());
        session.setLogs(new ArrayList<>());
        session.setWaitingForUserInput(true);
        session.setWaitingFor("script_confirmation");

        sessions.put(sessionId, session);

        // Step 1: 生成剧本
        addLog(session, "剧本生成", "正在分析您的需求并生成剧本...", null, false);
        
        VideoPlan scriptPlan = videoPlanner.generateScript(prompt, session.getStyle(), modelName);
        session.setScriptPlan(scriptPlan);

        if (scriptPlan.isSuccess()) {
            addLog(session, "剧本生成", 
                String.format("剧本生成完成！共 %d 个场景", scriptPlan.getScenes().size()),
                scriptPlan.getScenes(), true);
            
            // 询问用户是否需要补充
            addLog(session, "用户确认", 
                "请查看以上场景列表，如果需要添加、修改或删除场景，请告诉我。如果满意，请回复'确认'开始生成图片。",
                null, false);
        } else {
            session.setState(AgentState.ERROR);
            addLog(session, "错误", "剧本生成失败: " + scriptPlan.getMessage(), null, false);
        }

        return session;
    }

    public XixiSession confirmScript(String sessionId, String userResponse, String modelName) {
        XixiSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        if ("确认".equals(userResponse) || "开始".equals(userResponse)) {
            session.setWaitingForUserInput(false);
            session.setWaitingFor(null);
            
            // 开始优化每个场景的提示词
            addLog(session, "提示词优化", "开始优化每个场景的绘画提示词...", null, false);
            
            List<Scene> scenes = session.getScriptPlan().getScenes();
            for (int i = 0; i < scenes.size(); i++) {
                Scene scene = scenes.get(i);
                addLog(session, "提示词优化", 
                    String.format("正在优化场景 %d/%d: %s", i + 1, scenes.size(), 
                        scene.getDescription().substring(0, Math.min(50, scene.getDescription().length())) + "..."),
                    null, false);
                
                // 使用多Agent协作优化提示词
                OptimizationResult optResult = promptOptimizer.optimizePrompt(
                    scene.getDescription(), 
                    session.getStyle(), 
                    modelName
                );
                
                SceneOptimization sceneOpt = new SceneOptimization();
                sceneOpt.setSceneNumber(i + 1);
                sceneOpt.setOriginalDescription(scene.getDescription());
                sceneOpt.setOptimizationResult(optResult);
                sceneOpt.setCompleted(false);
                
                session.getSceneOptimizations().add(sceneOpt);
                scene.setPrompt(optResult.getOptimizedPrompt());
                
                // 记录优化过程
                addLog(session, "提示词优化", 
                    String.format("场景 %d 优化完成！满意度: %.1f%% (第 %d 轮)", 
                        i + 1, optResult.getSatisfactionScore(), optResult.getIterationCount()),
                    optResult, true);
            }
            
            addLog(session, "提示词优化", "所有场景提示词优化完成！准备开始生成图片。", null, false);
            
            // 开始生成图片
            return generateImages(sessionId, modelName);
        } else {
            // 用户有补充或修改
            addLog(session, "用户输入", "收到您的修改: " + userResponse, null, false);
            // TODO: 使用AI理解用户修改意图并更新剧本
            addLog(session, "剧本更新", "剧本已根据您的要求更新，请再次确认。", 
                session.getScriptPlan().getScenes(), true);
            session.setWaitingForUserInput(true);
            session.setWaitingFor("script_confirmation");
            return session;
        }
    }

    public XixiSession generateImages(String sessionId, String modelName) {
        XixiSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        addLog(session, "图片生成", "开始生成场景图片...", null, false);
        
        AiModel model = modelFactory.getModel(modelName);
        List<Scene> scenes = session.getScriptPlan().getScenes();
        
        for (int i = 0; i < scenes.size(); i++) {
            Scene scene = scenes.get(i);
            SceneOptimization sceneOpt = session.getSceneOptimizations().get(i);
            
            addLog(session, "图片生成", 
                String.format("正在生成场景 %d/%d 的图片...", i + 1, scenes.size()),
                null, false);
            
            try {
                Map<String, Object> params = Map.of(
                        "prompt", scene.getPrompt(),
                        "style", session.getStyle(),
                        "resolution", "1024x1024"
                );
                
                String imageUrl = model.generateImage(params);
                sceneOpt.setGeneratedImageUrl(imageUrl);
                sceneOpt.setCompleted(true);
                session.getGeneratedImageUrls().add(imageUrl);
                
                addLog(session, "图片生成", 
                    String.format("场景 %d 图片生成完成！", i + 1),
                    imageUrl, false);
                    
            } catch (Exception e) {
                log.error("场景 {} 图片生成失败", i + 1, e);
                addLog(session, "图片生成", 
                    String.format("场景 %d 图片生成失败: %s", i + 1, e.getMessage()),
                    null, false);
            }
        }
        
        addLog(session, "图片生成", "所有场景图片生成完成！", session.getGeneratedImageUrls(), true);
        
        // 进入视频流程规划阶段
        session.setWaitingForUserInput(true);
        session.setWaitingFor("video_flow_confirmation");
        
        return planVideoFlow(sessionId, modelName);
    }

    public XixiSession planVideoFlow(String sessionId, String modelName) {
        XixiSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        addLog(session, "视频规划", "正在规划视频制作流程...", null, false);
        
        VideoPlan videoPlan = videoPlanner.planVideoFlow(
            session.getGeneratedImageUrls(),
            session.getScriptPlan(),
            modelName
        );
        
        session.setVideoFlowPlan(videoPlan);
        
        if (videoPlan.isSuccess()) {
            addLog(session, "视频规划", 
                String.format("视频流程规划完成！总时长: %s，共 %d 个片段", 
                    videoPlan.getDuration(), videoPlan.getVideoSegments().size()),
                videoPlan, true);
            
            addLog(session, "用户确认", 
                "请查看以上视频流程。接下来需要选择视频生成方式：\n" +
                "1. 【图生视频】- 使用刚才生成的图片序列生成视频（推荐）\n" +
                "2. 【直接生成】- 根据剧本描述直接生成视频\n" +
                "如果需要调整时长、转场效果或添加特效，也可以告诉我。\n" +
                "请回复'图生视频'或'直接生成'开始生成。",
                null, false);
        } else {
            addLog(session, "错误", "视频流程规划失败: " + videoPlan.getMessage(), null, false);
        }
        
        return session;
    }

    public XixiSession confirmVideoFlow(String sessionId, String userResponse, String modelName) {
        XixiSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        // 用户选择图生视频方式
        if ("图生视频".equals(userResponse)) {
            session.setWaitingForUserInput(false);
            session.setWaitingFor(null);
            addLog(session, "用户选择", "您选择了【图生视频】方式", null, false);
            return generateVideo(sessionId, modelName);
        } 
        // 用户选择直接生成方式
        else if ("直接生成".equals(userResponse)) {
            session.setWaitingForUserInput(false);
            session.setWaitingFor(null);
            addLog(session, "用户选择", "您选择了【直接生成】方式", null, false);
            return generateVideoDirectly(sessionId, modelName);
        }
        // 传统确认方式，默认使用图生视频
        else if ("确认".equals(userResponse) || "开始".equals(userResponse)) {
            session.setWaitingForUserInput(false);
            session.setWaitingFor(null);
            addLog(session, "用户选择", "默认使用【图生视频】方式", null, false);
            return generateVideo(sessionId, modelName);
        }
        // 其他输入视为修改请求
        else {
            addLog(session, "用户输入", "收到您的修改: " + userResponse, null, false);
            // TODO: 使用AI理解用户修改意图并更新视频流程
            addLog(session, "视频规划", "视频流程已根据您的要求更新，请再次确认。", 
                session.getVideoFlowPlan(), true);
            session.setWaitingForUserInput(true);
            session.setWaitingFor("video_flow_confirmation");
            return session;
        }
    }

    /**
     * 根据剧本描述直接生成视频（不使用图片）
     */
    public XixiSession generateVideoDirectly(String sessionId, String modelName) {
        XixiSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        addLog(session, "视频生成", "根据剧本描述直接生成视频...", null, false);
        
        AiModel model = modelFactory.getModel(modelName);
        
        try {
            // 构建剧本描述
            StringBuilder scriptDesc = new StringBuilder();
            for (Scene scene : session.getScriptPlan().getScenes()) {
                scriptDesc.append(scene.getDescription()).append("\n");
            }
            
            Map<String, Object> params = Map.of(
                    "prompt", scriptDesc.toString(),
                    "style", session.getStyle(),
                    "duration", session.getVideoFlowPlan().getDuration()
            );
            
            String videoUrl = model.createVideoFromScript(params);
            session.setFinalVideoUrl(videoUrl);
            session.setState(AgentState.FINISHED);
            
            addLog(session, "视频生成", "视频生成完成！", videoUrl, false);
            addLog(session, "完成", "嘻嘻模式执行完成！您的视频已生成。", null, false);
            
        } catch (Exception e) {
            log.error("直接视频生成失败", e);
            session.setState(AgentState.ERROR);
            addLog(session, "错误", "视频生成失败: " + e.getMessage(), null, false);
        }
        
        return session;
    }

    public XixiSession generateVideo(String sessionId, String modelName) {
        XixiSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }

        addLog(session, "视频生成", "开始生成最终视频...", null, false);
        
        AiModel model = modelFactory.getModel(modelName);
        
        try {
            Map<String, Object> params = Map.of(
                    "imageUrls", session.getGeneratedImageUrls(),
                    "segments", session.getVideoFlowPlan().getVideoSegments(),
                    "transitionStyle", session.getVideoFlowPlan().getTransitionStyle(),
                    "duration", session.getVideoFlowPlan().getDuration()
            );
            
            String videoUrl = model.createVideo(params);
            session.setFinalVideoUrl(videoUrl);
            session.setState(AgentState.FINISHED);
            
            addLog(session, "视频生成", "视频生成完成！", videoUrl, false);
            addLog(session, "完成", "嘻嘻模式执行完成！您的视频已生成。", null, false);
            
        } catch (Exception e) {
            log.error("视频生成失败", e);
            session.setState(AgentState.ERROR);
            addLog(session, "错误", "视频生成失败: " + e.getMessage(), null, false);
        }
        
        return session;
    }

    public XixiSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    private void addLog(XixiSession session, String stage, String message, Object data, boolean collapsible) {
        ProcessLog log = new ProcessLog();
        log.setTimestamp(System.currentTimeMillis());
        log.setStage(stage);
        log.setMessage(message);
        log.setData(data);
        log.setCollapsible(collapsible);
        session.getLogs().add(log);
    }
}
