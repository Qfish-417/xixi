package com.xixi.manjuagent.agent;

import cn.hutool.core.util.StrUtil;
import com.xixi.manjuagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends BaseAgent {

    private final ToolInterface[] availableTools;
    private String lastResult;

    public ToolCallAgent(ToolInterface[] availableTools) {
        this.availableTools = availableTools;
    }

    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            Message userMessage = new Message("user", getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        try {
            String userInput = getNextStepPrompt() != null ? getNextStepPrompt() : "";
            
            StringBuilder toolsInfo = new StringBuilder();
            for (ToolInterface tool : availableTools) {
                toolsInfo.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            }
            
            String thinkingResult = analyzeInput(userInput, toolsInfo.toString());
            this.lastResult = thinkingResult;
            
            log.info(getName() + "的思考：" + thinkingResult);
            
            for (ToolInterface tool : availableTools) {
                if (thinkingResult.contains(tool.getName())) {
                    getMessageList().add(new Message("assistant", thinkingResult));
                    return true;
                }
            }
            
            getMessageList().add(new Message("assistant", thinkingResult));
            return false;
            
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new Message("assistant", "处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    private String analyzeInput(String userInput, String toolsInfo) {
        if (userInput.contains("生成图片") || userInput.contains("文生图")) {
            return "用户需要生成图片，调用textToImage工具";
        } else if (userInput.contains("修改图片") || userInput.contains("图生图")) {
            return "用户需要修改图片，调用imageToImage工具";
        } else if (userInput.contains("视频") || userInput.contains("图生视频")) {
            return "用户需要生成视频，调用imageToVideo工具";
        } else if (userInput.contains("脚本") || userInput.contains("剧本")) {
            return "用户需要生成脚本，调用scriptGenerator工具";
        } else if (userInput.contains("结束") || userInput.contains("终止")) {
            return "用户要求结束任务，调用doTerminate工具";
        } else {
            return "用户输入：" + userInput + "\n可用工具：\n" + toolsInfo + "\n请告诉我您需要使用哪个工具？";
        }
    }

    @Override
    public String act() {
        if (lastResult == null || !lastResult.contains("调用")) {
            return "没有工具需要调用";
        }

        StringBuilder results = new StringBuilder();
        
        for (ToolInterface tool : availableTools) {
            if (lastResult.contains(tool.getName())) {
                try {
                    Object response = tool.execute(createMockParams(tool.getName()));
                    String result = "工具 " + tool.getName() + " 返回的结果：" + response;
                    results.append(result).append("\n");
                    
                    getMessageList().add(new Message("assistant", result));
                    
                    if ("doTerminate".equals(tool.getName())) {
                        setState(AgentState.FINISHED);
                    }
                } catch (Exception e) {
                    results.append("工具 ").append(tool.getName()).append(" 执行失败：").append(e.getMessage()).append("\n");
                }
            }
        }

        log.info(results.toString());
        return results.toString();
    }

    private Map<String, Object> createMockParams(String toolName) {
        Map<String, Object> params = new HashMap<>();
        
        switch (toolName) {
            case "textToImage":
                params.put("prompt", "一幅美丽的动漫风格图片");
                params.put("style", "anime");
                params.put("resolution", "1024x1024");
                break;
            case "imageToImage":
                params.put("imageUrl", "https://example.com/image.png");
                params.put("prompt", "转换为卡通风格");
                params.put("strength", 0.7);
                break;
            case "imageToVideo":
                params.put("imageUrls", new ArrayList<>(List.of("https://example.com/img1.png", "https://example.com/img2.png")));
                params.put("duration", 10);
                params.put("fps", 24);
                break;
            case "scriptGenerator":
                params.put("theme", "校园青春故事");
                params.put("style", "日系动漫");
                params.put("episodes", 3);
                break;
            case "doTerminate":
                params.put("reason", "用户请求终止");
                break;
        }
        
        return params;
    }

    @Override
    public String step() {
        boolean needsAction = think();
        if (needsAction) {
            return act();
        }
        return "思考完成，无需执行工具";
    }

    public interface ToolInterface {
        String getName();
        String getDescription();
        Object execute(Map<String, Object> parameters);
    }
}
