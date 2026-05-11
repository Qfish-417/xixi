package com.xixi.manjuagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI漫剧Agent启动类
 */
@SpringBootApplication
@EnableScheduling
public class ManjuAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManjuAgentApplication.class, args);
    }
}