package com.xixi.manjuagent.tools;

import com.xixi.manjuagent.agent.ToolCallAgent.ToolInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ScriptGeneratorTool implements ToolInterface {

    @Override
    public String getName() {
        return "scriptGenerator";
    }

    @Override
    public String getDescription() {
        return "脚本生成工具：根据主题生成动漫剧本";
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        String theme = (String) parameters.get("theme");
        String style = parameters.get("style") != null ? (String) parameters.get("style") : "日系动漫";
        Integer episodes = parameters.get("episodes") != null ? ((Number) parameters.get("episodes")).intValue() : 3;
        
        log.info("调用脚本生成工具: theme={}, style={}, episodes={}", theme, style, episodes);
        
        StringBuilder script = new StringBuilder();
        script.append("=== ").append(theme).append(" ===").append("\n\n");
        for (int i = 1; i <= episodes; i++) {
            script.append("第").append(i).append("集：").append("故事发展").append(i).append("\n");
            script.append("场景：校园教室\n");
            script.append("角色：主角、配角\n");
            script.append("剧情：日常校园生活片段...\n\n");
        }
        
        log.info("脚本生成完成");
        return script.toString();
    }
}
