package com.jam.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 游戏应用启动类。
 * 通过 scanBasePackages 扫描整个 com.jam 包（含 agent-platform 的平台 Bean 与 game-app 的游戏 Bean），
 * 形成"模块化单体"：同一进程直接调用平台与游戏业务，不走 RPC。
 */
@SpringBootApplication(scanBasePackages = "com.jam")
public class GameApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);
    }
}