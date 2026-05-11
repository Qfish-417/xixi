package com.xixi.manjuagent.tools;

import com.xixi.manjuagent.agent.ToolCallAgent.ToolInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {

    @Bean
    public ToolInterface[] allTools(
            TextToImageTool textToImageTool,
            ImageToImageTool imageToImageTool,
            ImageToVideoTool imageToVideoTool,
            ScriptGeneratorTool scriptGeneratorTool,
            TerminateTool terminateTool) {
        return new ToolInterface[]{
                textToImageTool,
                imageToImageTool,
                imageToVideoTool,
                scriptGeneratorTool,
                terminateTool
        };
    }
}
