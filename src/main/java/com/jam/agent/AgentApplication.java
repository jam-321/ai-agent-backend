package com.jam.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.jam.agent.agent.persistence.mapper",
        "com.jam.agent.auth.persistence.mapper",
        "com.jam.agent.common.database.mapper",
        "com.jam.agent.conversation.persistence.mapper",
        "com.jam.agent.monitoring.persistence.mapper"
})
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
