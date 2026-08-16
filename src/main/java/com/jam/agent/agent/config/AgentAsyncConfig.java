package com.jam.agent.agent.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAsyncConfig {

    @Bean(name = "agentRunExecutor")
    Executor agentRunExecutor(AgentProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getExecutor().getRunCoreSize());
        executor.setMaxPoolSize(properties.getExecutor().getRunMaxSize());
        executor.setQueueCapacity(properties.getExecutor().getRunQueueCapacity());
        executor.setThreadNamePrefix("agent-run-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    @Bean(name = "agentToolExecutor")
    Executor agentToolExecutor(AgentProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getExecutor().getToolCoreSize());
        executor.setMaxPoolSize(properties.getExecutor().getToolMaxSize());
        executor.setQueueCapacity(properties.getExecutor().getToolQueueCapacity());
        executor.setThreadNamePrefix("agent-tool-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
