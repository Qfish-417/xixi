package com.xixi.manjuagent.tools;

import com.xixi.manjuagent.agent.ToolCallAgent.ToolInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class TerminateTool implements ToolInterface {

    @Override
    public String getName() {
        return "doTerminate";
    }

    @Override
    public String getDescription() {
        return "终止工具：结束当前任务";
    }

    @Override
    public Object execute(Map<String, Object> parameters) {
        String reason = parameters.get("reason") != null ? (String) parameters.get("reason") : "用户请求";
        
        log.info("调用终止工具: reason={}", reason);
        
        return "任务已终止，原因：" + reason;
    }
}
