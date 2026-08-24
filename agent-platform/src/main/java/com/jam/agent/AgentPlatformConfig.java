package com.jam.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agent 平台配置：由 game-app 启动时通过组件扫描加载。
 * 原启动类改造为平台配置，避免与 game-app 的启动类重复定义 @SpringBootApplication。
 */
@Configuration
@EnableScheduling
@MapperScan({
        "com.jam.agent.agent.persistence.mapper",
        "com.jam.agent.agent.model.persistence.mapper",
        "com.jam.agent.auth.persistence.mapper",
        "com.jam.agent.common.audit",
        "com.jam.agent.common.database.mapper",
        "com.jam.agent.conversation.persistence.mapper",
        "com.jam.agent.monitoring.persistence.mapper"
})
public class AgentPlatformConfig {
}