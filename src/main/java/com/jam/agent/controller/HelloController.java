package com.jam.agent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础联通性检查，不依赖 LLM，用于验证后端已启动。
 */
@RestController
public class HelloController {

    @GetMapping("/api/ping")
    public String ping() {
        return "pong！";
    }
}
