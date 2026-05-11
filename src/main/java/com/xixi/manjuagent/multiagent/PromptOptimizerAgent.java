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
public class PromptOptimizerAgent {

    private final ModelFactory modelFactory;

    @Data
    public static class OptimizationResult {
        private String originalPrompt;
        private String optimizedPrompt;
        private String imaginedDescription;
        private double satisfactionScore;
        private int iterationCount;
        private List<IterationLog> iterationLogs;
        private boolean success;
        private String message;
    }

    @Data
    public static class IterationLog {
        private int iteration;
        private String prompt;
        private String imaginedDescription;
        private double satisfactionScore;
    }

    private static final String SYSTEM_PROMPT_OPTIMIZER = """
        你是一个专业的AI绘画提示词优化专家。你的任务是将用户的简单描述优化成详细的、高质量的AI绘画提示词。
        
        请遵循以下原则：
        1. 添加详细的视觉细节（光影、色彩、构图、风格）
        2. 指定艺术风格（动漫、写实、油画、水彩等）
        3. 添加质量修饰词（ masterpiece, best quality, highly detailed 等）
        4. 保持原始意图的同时增强表现力
        5. 使用英文输出，这是AI绘画模型的标准
        
        输出格式：
        优化后的提示词：[英文提示词]
        中文说明：[中文解释]
        """;

    private static final String SYSTEM_PROMPT_IMAGINE = """
        你是一个视觉想象力丰富的AI助手。当你看到一个绘画提示词时，你需要想象这个提示词会生成什么样的图像。
        
        请详细描述：
        1. 画面主体是什么
        2. 背景环境如何
        3. 光影效果如何
        4. 色彩氛围怎样
        5. 整体风格特点
        6. 画面构图
        
        用生动的语言描述你想象中的画面，就像你在看一张真实的图片一样。
        """;

    private static final String SYSTEM_PROMPT_COMPARE = """
        你是一个严格的图像质量评估专家。你需要比较两个描述：
        1. 原始提示词的意图
        2. 想象Agent根据优化后提示词想象出的画面
        
        评估维度（每项0-25分，总分100分）：
        - 主题一致性：想象画面是否准确反映原始意图
        - 细节丰富度：画面细节是否充足
        - 艺术表现力：画面是否具有艺术美感
        - 创意契合度：优化是否增强了原始创意
        
        输出格式：
        评分：XX/100
        评估说明：[详细说明]
        是否通过：[是/否，96分以上为通过]
        """;

    public OptimizationResult optimizePrompt(String originalPrompt, String style, String modelName) {
        log.info("开始优化提示词: {}", originalPrompt);
        
        OptimizationResult result = new OptimizationResult();
        result.setOriginalPrompt(originalPrompt);
        result.setIterationLogs(new ArrayList<>());
        result.setIterationCount(0);
        
        AiModel model = modelFactory.getModel(modelName);
        String currentPrompt = originalPrompt;
        
        OptimizationResult bestResult = null;
        double bestScore = 0;
        
        for (int i = 1; i <= 10; i++) {
            result.setIterationCount(i);
            log.info("第 {} 轮优化", i);
            
            // Step 1: 优化提示词
            String optimizedPrompt = optimize(model, currentPrompt, style);
            
            // Step 2: 想象画面
            String imaginedDescription = imagine(model, optimizedPrompt);
            
            // Step 3: 比对评估
            double satisfactionScore = compare(model, originalPrompt, optimizedPrompt, imaginedDescription);
            
            IterationLog iterationLog = new IterationLog();
            iterationLog.setIteration(i);
            iterationLog.setPrompt(optimizedPrompt);
            iterationLog.setImaginedDescription(imaginedDescription);
            iterationLog.setSatisfactionScore(satisfactionScore);
            result.getIterationLogs().add(iterationLog);
            
            log.info("第 {} 轮满意度: {}%", i, satisfactionScore);
            
            // 记录最佳结果
            if (satisfactionScore > bestScore) {
                bestScore = satisfactionScore;
                bestResult = new OptimizationResult();
                bestResult.setOptimizedPrompt(optimizedPrompt);
                bestResult.setImaginedDescription(imaginedDescription);
                bestResult.setSatisfactionScore(satisfactionScore);
                bestResult.setIterationCount(i);
            }
            
            // 达到96%以上，直接通过
            if (satisfactionScore >= 96.0) {
                result.setSuccess(true);
                result.setOptimizedPrompt(optimizedPrompt);
                result.setImaginedDescription(imaginedDescription);
                result.setSatisfactionScore(satisfactionScore);
                result.setMessage(String.format("第 %d 轮达到满意度 %.1f%%，优化成功", i, satisfactionScore));
                return result;
            }
            
            // 基于反馈继续优化
            currentPrompt = optimizedPrompt + "\n\n上一轮想象的画面：" + imaginedDescription + 
                    "\n\n需要改进的地方：" + generateImprovementFeedback(model, originalPrompt, imaginedDescription);
        }
        
        // 10轮后选择最佳结果
        result.setSuccess(bestScore >= 80);
        result.setOptimizedPrompt(bestResult.getOptimizedPrompt());
        result.setImaginedDescription(bestResult.getImaginedDescription());
        result.setSatisfactionScore(bestResult.getSatisfactionScore());
        result.setMessage(String.format("10轮优化完成，选择最佳结果（第 %d 轮，满意度 %.1f%%）", 
                bestResult.getIterationCount(), bestResult.getSatisfactionScore()));
        
        return result;
    }

    private String optimize(AiModel model, String prompt, String style) {
        String fullPrompt = SYSTEM_PROMPT_OPTIMIZER + "\n\n用户原始描述：" + prompt + 
                "\n期望风格：" + style + "\n\n请优化这个提示词：";
        
        Map<String, Object> params = Map.of(
                "prompt", fullPrompt,
                "maxTokens", 500,
                "temperature", 0.7
        );
        
        return model.generateText(params);
    }

    private String imagine(AiModel model, String optimizedPrompt) {
        String fullPrompt = SYSTEM_PROMPT_IMAGINE + "\n\n绘画提示词：" + optimizedPrompt + 
                "\n\n请详细描述你想象中的画面：";
        
        Map<String, Object> params = Map.of(
                "prompt", fullPrompt,
                "maxTokens", 600,
                "temperature", 0.8
        );
        
        return model.generateText(params);
    }

    private double compare(AiModel model, String original, String optimized, String imagined) {
        String fullPrompt = SYSTEM_PROMPT_COMPARE + 
                "\n\n原始意图：" + original +
                "\n\n优化后提示词：" + optimized +
                "\n\n想象出的画面：" + imagined +
                "\n\n请评估并给出评分：";
        
        Map<String, Object> params = Map.of(
                "prompt", fullPrompt,
                "maxTokens", 400,
                "temperature", 0.3
        );
        
        String response = model.generateText(params);
        
        // 解析评分
        try {
            if (response.contains("评分：")) {
                String scoreStr = response.substring(response.indexOf("评分：") + 3);
                scoreStr = scoreStr.substring(0, scoreStr.indexOf("/")).trim();
                return Double.parseDouble(scoreStr);
            }
        } catch (Exception e) {
            log.warn("解析评分失败: {}", e.getMessage());
        }
        
        return 0;
    }

    private String generateImprovementFeedback(AiModel model, String original, String imagined) {
        String prompt = "原始意图：" + original + "\n\n实际想象的画面：" + imagined + 
                "\n\n请指出需要改进的地方，用简短的要点列出：";
        
        Map<String, Object> params = Map.of(
                "prompt", prompt,
                "maxTokens", 200,
                "temperature", 0.5
        );
        
        return model.generateText(params);
    }
}
