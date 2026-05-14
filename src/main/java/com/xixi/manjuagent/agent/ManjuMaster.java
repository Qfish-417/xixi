package com.xixi.manjuagent.agent;

import com.xixi.manjuagent.agent.model.TaskResult;
import com.xixi.manjuagent.circuit.CircuitBreaker;
import com.xixi.manjuagent.circuit.RateLimiter;
import com.xixi.manjuagent.multiagent.TaskPlan;
import com.xixi.manjuagent.model.AiModel;
import com.xixi.manjuagent.model.ModelFactory;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ManjuMaster {

    private static final String SYSTEM_PROMPT = """
            你是ManjuMaster，AI漫剧创作的主智能体。
            你的职责是：
            1. 理解用户的自然语言需求
            2. 分析意图并提取关键信息
            3. 规划执行步骤
            4. 调用合适的工具完成任务
            """;

    private final ModelFactory modelFactory;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    public ManjuMaster(ModelFactory modelFactory,
                      CircuitBreaker circuitBreaker,
                      RateLimiter rateLimiter) {
        this.modelFactory = modelFactory;
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
        System.out.println("[TRACE] ManjuMaster 初始化完成");
    }

    public TaskResult process(String userInput) {
        return process(userInput, null);
    }

    public TaskResult process(String userInput, String modelName) {
        System.out.println("[TRACE-1] ===== 主智能体入口 =====");
        System.out.println("[TRACE-1] input=" + userInput + ", modelName=" + modelName);

        if (!rateLimiter.tryAcquire("manju-master")) {
            System.out.println("[TRACE-1] !!! 限流拦截 !!!");
            return TaskResult.builder()
                    .success(false)
                    .message("请求过于频繁，请稍后重试")
                    .build();
        }

        try {
            IntentAnalysis analysis = analyzeInput(userInput);
            System.out.println("[TRACE-2] 意图分析: type=" + analysis.getIntentType()
                    + ", theme=" + analysis.getTheme()
                    + ", style=" + analysis.getStyle());

            TaskPlan plan = planTask(analysis, modelName);
            System.out.println("[TRACE-3] 任务计划: taskId=" + plan.getTaskId()
                    + ", 步骤数=" + plan.getSteps().size());
            for (TaskPlan.TaskStep s : plan.getSteps()) {
                System.out.println("[TRACE-3]   步骤" + s.getStepNumber()
                        + ": tool=" + s.getToolName()
                        + ", model=" + s.getModelName()
                        + ", params=" + s.getParameters());
            }

            TaskResult result = executePlan(plan);
            String msgPreview = result.getMessage() != null
                    ? result.getMessage().substring(0, Math.min(150, result.getMessage().length()))
                    : "null";
            System.out.println("[TRACE-6] 最终返回: success=" + result.isSuccess()
                    + ", message=" + msgPreview
                    + ", images=" + (result.getImages() != null ? result.getImages().size() + "张" : "无")
                    + ", videoUrl=" + (result.getVideoUrl() != null ? "有" : "无"));
            return result;

        } catch (Exception e) {
            System.out.println("[TRACE-ERR] 主智能体执行失败: " + e.getMessage());
            e.printStackTrace();
            return TaskResult.builder()
                    .success(false)
                    .message("执行失败: " + e.getMessage())
                    .build();
        }
    }

    public IntentAnalysis analyzeInput(String input) {
        IntentAnalysis.IntentType intentType = detectIntent(input);

        Map<String, Object> params = extractParameters(input);
        String theme = (String) params.getOrDefault("theme", input);
        String style = (String) params.getOrDefault("style", "日系动漫");

        return IntentAnalysis.builder()
                .intentType(intentType)
                .originalInput(input)
                .theme(theme)
                .style(style)
                .parameters(params)
                .description("用户需求分析完成")
                .build();
    }

    private IntentAnalysis.IntentType detectIntent(String input) {
        input = input.toLowerCase();

        if ((input.contains("生成") && input.contains("图片")) ||
            (input.contains("画") && input.contains("图"))) {
            return IntentAnalysis.IntentType.TEXT_TO_IMAGE;
        }

        if ((input.contains("修改") && input.contains("图")) ||
            (input.contains("转换") && input.contains("风格"))) {
            return IntentAnalysis.IntentType.IMAGE_TO_IMAGE;
        }

        if (input.contains("视频") || input.contains("动画")) {
            if (input.contains("图片") || input.contains("图")) {
                return IntentAnalysis.IntentType.IMAGE_TO_VIDEO;
            }
        }

        if (input.contains("脚本") || input.contains("故事")) {
            return IntentAnalysis.IntentType.SCRIPT_GENERATION;
        }

        if (input.contains("漫剧") || input.contains("创作") ||
            input.contains("完整") || input.contains("一集")) {
            return IntentAnalysis.IntentType.FULL_PRODUCTION;
        }

        if (input.contains("?") || input.contains("？") ||
            input.contains("怎么") || input.contains("如何")) {
            return IntentAnalysis.IntentType.QUESTION;
        }

        return IntentAnalysis.IntentType.UNKNOWN;
    }

    private Map<String, Object> extractParameters(String input) {
        Map<String, Object> params = new HashMap<>();

        Pattern stylePattern = Pattern.compile("(风格|画风|类型)[:：]?\\s*(\\S+)");
        Matcher styleMatcher = stylePattern.matcher(input);
        if (styleMatcher.find()) {
            params.put("style", styleMatcher.group(2));
        }

        Pattern countPattern = Pattern.compile("(\\d+)(集|张|个)");
        Matcher countMatcher = countPattern.matcher(input);
        if (countMatcher.find()) {
            params.put("count", Integer.parseInt(countMatcher.group(1)));
        }

        return params;
    }

    public TaskPlan planTask(IntentAnalysis analysis) {
        return planTask(analysis, null);
    }

    public TaskPlan planTask(IntentAnalysis analysis, String modelName) {
        String taskId = "task-" + System.currentTimeMillis();
        List<TaskPlan.TaskStep> steps = new ArrayList<>();

        switch (analysis.getIntentType()) {
            case TEXT_TO_IMAGE:
                steps.add(createTextToImageStep(1, analysis, modelName));
                break;

            case IMAGE_TO_IMAGE:
                steps.add(createImageToImageStep(1, analysis, modelName));
                break;

            case IMAGE_TO_VIDEO:
                steps.add(createImageToVideoStep(1, analysis, modelName));
                break;

            case SCRIPT_GENERATION:
                steps.add(createScriptStep(1, analysis, modelName));
                break;

            case FULL_PRODUCTION:
                steps.add(createScriptStep(1, analysis, modelName));
                steps.add(createTextToImageStep(2, analysis, modelName));
                steps.add(createImageToVideoStep(3, analysis, modelName));
                break;

            case QUESTION:
                steps.add(createAnswerStep(1, analysis, modelName));
                break;

            default:
                steps.add(createUnknownStep(1, analysis, modelName));
        }

        return TaskPlan.builder()
                .taskId(taskId)
                .prompt(analysis.getOriginalInput())
                .steps(steps)
                .build();
    }

    private TaskPlan.TaskStep createScriptStep(int stepNum, IntentAnalysis analysis) {
        return createScriptStep(stepNum, analysis, null);
    }

    private TaskPlan.TaskStep createScriptStep(int stepNum, IntentAnalysis analysis, String modelName) {
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "jimeng";
        return TaskPlan.TaskStep.builder()
                .stepNumber(stepNum)
                .toolName("scriptGenerator")
                .modelName(effectiveModel)
                .parameters(Map.of(
                        "theme", analysis.getTheme(),
                        "style", analysis.getStyle(),
                        "episodes", analysis.getCount() != null ? analysis.getCount() : 1
                ))
                .dependencies(List.of())
                .status("pending")
                .build();
    }

    private TaskPlan.TaskStep createTextToImageStep(int stepNum, IntentAnalysis analysis) {
        return createTextToImageStep(stepNum, analysis, null);
    }

    private TaskPlan.TaskStep createTextToImageStep(int stepNum, IntentAnalysis analysis, String modelName) {
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "jimeng";
        return TaskPlan.TaskStep.builder()
                .stepNumber(stepNum)
                .toolName("textToImage")
                .modelName(effectiveModel)
                .parameters(Map.of(
                        "prompt", analysis.getTheme(),
                        "style", analysis.getStyle(),
                        "resolution", "1920x1920"
                ))
                .dependencies(stepNum > 1 ? List.of(stepNum - 1) : List.of())
                .status("pending")
                .build();
    }

    private TaskPlan.TaskStep createImageToImageStep(int stepNum, IntentAnalysis analysis) {
        return createImageToImageStep(stepNum, analysis, null);
    }

    private TaskPlan.TaskStep createImageToImageStep(int stepNum, IntentAnalysis analysis, String modelName) {
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "jimeng";
        return TaskPlan.TaskStep.builder()
                .stepNumber(stepNum)
                .toolName("imageToImage")
                .modelName(effectiveModel)
                .parameters(Map.of(
                        "imageUrl", analysis.getParameters().getOrDefault("imageUrl", ""),
                        "prompt", analysis.getTheme(),
                        "strength", 0.7
                ))
                .dependencies(List.of())
                .status("pending")
                .build();
    }

    private TaskPlan.TaskStep createImageToVideoStep(int stepNum, IntentAnalysis analysis) {
        return createImageToVideoStep(stepNum, analysis, null);
    }

    private TaskPlan.TaskStep createImageToVideoStep(int stepNum, IntentAnalysis analysis, String modelName) {
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "jimeng";
        return TaskPlan.TaskStep.builder()
                .stepNumber(stepNum)
                .toolName("imageToVideo")
                .modelName(effectiveModel)
                .parameters(Map.of(
                        "duration", 10,
                        "fps", 24
                ))
                .dependencies(List.of(stepNum - 1))
                .status("pending")
                .build();
    }

    private TaskPlan.TaskStep createAnswerStep(int stepNum, IntentAnalysis analysis) {
        return createAnswerStep(stepNum, analysis, null);
    }

    private TaskPlan.TaskStep createAnswerStep(int stepNum, IntentAnalysis analysis, String modelName) {
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "deepseek";
        return TaskPlan.TaskStep.builder()
                .stepNumber(stepNum)
                .toolName("question")
                .modelName(effectiveModel)
                .parameters(Map.of("question", analysis.getOriginalInput()))
                .dependencies(List.of())
                .status("pending")
                .build();
    }

    private TaskPlan.TaskStep createUnknownStep(int stepNum, IntentAnalysis analysis) {
        return createUnknownStep(stepNum, analysis, null);
    }

    private TaskPlan.TaskStep createUnknownStep(int stepNum, IntentAnalysis analysis, String modelName) {
        String effectiveModel = (modelName != null && !modelName.isEmpty()) ? modelName : "deepseek";
        return TaskPlan.TaskStep.builder()
                .stepNumber(stepNum)
                .toolName("unknown")
                .modelName(effectiveModel)
                .parameters(Map.of("input", analysis.getOriginalInput()))
                .dependencies(List.of())
                .status("pending")
                .build();
    }

    public TaskResult executePlan(TaskPlan plan) {
        List<TaskResult.StepResult> stepResults = new ArrayList<>();
        String mainMessage = null;
        List<String> imageUrls = new ArrayList<>();
        String videoUrl = null;

        for (TaskPlan.TaskStep step : plan.getSteps()) {
            System.out.println("[TRACE-4] 执行步骤" + step.getStepNumber() + "/" + plan.getSteps().size()
                    + ": tool=" + step.getToolName() + ", model=" + step.getModelName());

            Object resultData = executeStep(step);
            String resultMsg = "";

            if (resultData != null) {
                resultMsg = "成功";
                step.setResult(resultData);
                updateStepDependencies(plan, step, resultData);

                String preview = resultData.toString().length() > 200
                        ? resultData.toString().substring(0, 200) + "..."
                        : resultData.toString();
                boolean isTemplate = resultData.toString().contains("这是一段由")
                        || resultData.toString().contains("Deepseek生成的详细文本内容")
                        || resultData.toString().contains("基于提示词");
                System.out.println("[TRACE-4] 步骤" + step.getStepNumber() + "返回: 成功"
                        + ", 类型=" + resultData.getClass().getSimpleName()
                        + ", 是否模板=" + isTemplate
                        + ", 内容=" + preview);

                String toolName = step.getToolName();
                if ("textToImage".equals(toolName) || "imageToImage".equals(toolName)) {
                    imageUrls.add(resultData.toString());
                } else if ("imageToVideo".equals(toolName)) {
                    videoUrl = resultData.toString();
                } else {
                    mainMessage = resultData.toString();
                }
            } else {
                resultMsg = "失败";
                System.out.println("[TRACE-4] !!! 步骤" + step.getStepNumber() + "返回: 失败(null) !!!");
            }

            TaskResult.StepResult stepResult = TaskResult.StepResult.builder()
                    .stepNumber(step.getStepNumber())
                    .toolName(step.getToolName())
                    .result(resultMsg + ": " + (resultData != null ? resultData.toString() : "无结果"))
                    .timestamp(System.currentTimeMillis())
                    .build();
            stepResults.add(stepResult);

            if (resultData == null) {
                return TaskResult.builder()
                        .success(false)
                        .message("步骤 " + step.getStepNumber() + " 执行失败")
                        .step(step.getStepNumber())
                        .totalSteps(plan.getSteps().size())
                        .stepResults(stepResults)
                        .build();
            }
        }

        String message = mainMessage != null ? mainMessage : summarizeResult(stepResults);

        return TaskResult.builder()
                .success(true)
                .message(message)
                .images(imageUrls.isEmpty() ? null : imageUrls)
                .videoUrl(videoUrl)
                .step(plan.getSteps().size())
                .totalSteps(plan.getSteps().size())
                .stepResults(stepResults)
                .build();
    }

    private Object executeStep(TaskPlan.TaskStep step) {
        String toolName = step.getToolName();
        String modelName = step.getModelName();
        Map<String, Object> parameters = step.getParameters();

        try {
            return circuitBreaker.execute(modelName, () -> {
                AiModel model = modelFactory.getModel(modelName);
                System.out.println("[TRACE-5] 调用模型: model=" + (model != null ? model.getName() : "null")
                        + ", tool=" + toolName + ", params=" + parameters);

                return switch (toolName) {
                    case "textToImage" -> model.generateImage(parameters);
                    case "imageToImage" -> model.transformImage(parameters);
                    case "imageToVideo" -> model.createVideo(parameters);
                    case "scriptGenerator" -> model.generateScript(parameters);
                    case "question", "unknown" -> model.generateText(parameters);
                    default -> throw new IllegalArgumentException("未知工具: " + toolName);
                };
            });
        } catch (Exception e) {
            System.out.println("[TRACE-5] !!! 执行步骤失败: tool=" + toolName + ", model=" + modelName
                    + ", 异常=" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void updateStepDependencies(TaskPlan plan, TaskPlan.TaskStep completedStep, Object result) {
        for (TaskPlan.TaskStep step : plan.getSteps()) {
            if (step.getDependencies() != null && step.getDependencies().contains(completedStep.getStepNumber())) {
                if (step.getParameters() == null) {
                    step.setParameters(new HashMap<>());
                }

                if (completedStep.getToolName().equals("scriptGenerator") && step.getToolName().equals("textToImage")) {
                    step.getParameters().put("prompt", result.toString());
                } else if (completedStep.getToolName().equals("textToImage") && step.getToolName().equals("imageToVideo")) {
                    step.getParameters().put("imageUrls", List.of(result.toString()));
                }
            }
        }
    }

    public String summarizeResult(List<TaskResult.StepResult> results) {
        if (results.isEmpty()) {
            return "未执行任何步骤";
        }

        StringBuilder sb = new StringBuilder("任务执行完成！\n");
        for (TaskResult.StepResult result : results) {
            sb.append("步骤 ").append(result.getStepNumber())
              .append(" (").append(result.getToolName()).append("): ")
              .append(result.getResult()).append("\n");
        }
        return sb.toString();
    }

    @Data
    @Builder
    public static class IntentAnalysis {
        private IntentType intentType;
        private String originalInput;
        private String theme;
        private String style;
        private Integer count;
        private Map<String, Object> parameters;
        private String description;

        public enum IntentType {
            TEXT_TO_IMAGE,
            IMAGE_TO_IMAGE,
            IMAGE_TO_VIDEO,
            SCRIPT_GENERATION,
            FULL_PRODUCTION,
            QUESTION,
            UNKNOWN
        }
    }
}
