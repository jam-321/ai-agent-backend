package com.jam.agent.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private final Loop loop = new Loop();
    private final Memory memory = new Memory();
    private final Progress progress = new Progress();
    private final Lock lock = new Lock();
    private final Executor executor = new Executor();

    public Loop getLoop() { return loop; }
    public Memory getMemory() { return memory; }
    public Progress getProgress() { return progress; }
    public Lock getLock() { return lock; }
    public Executor getExecutor() { return executor; }

    public static class Loop {
        private int maxAttempts = 2;
        private int maxToolRounds = 40;
        private int maxToolsPerRound = 8;
        private Duration maxRunDuration = Duration.ofMinutes(5);
        private int maxDegenerateRetries = 2;
        private int maxSameToolSignature = 3;
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { maxAttempts = v; }
        public int getMaxToolRounds() { return maxToolRounds; }
        public void setMaxToolRounds(int v) { maxToolRounds = v; }
        public int getMaxToolsPerRound() { return maxToolsPerRound; }
        public void setMaxToolsPerRound(int v) { maxToolsPerRound = v; }
        public Duration getMaxRunDuration() { return maxRunDuration; }
        public void setMaxRunDuration(Duration v) { maxRunDuration = v; }
        public int getMaxDegenerateRetries() { return maxDegenerateRetries; }
        public void setMaxDegenerateRetries(int v) { maxDegenerateRetries = v; }
        public int getMaxSameToolSignature() { return maxSameToolSignature; }
        public void setMaxSameToolSignature(int v) { maxSameToolSignature = v; }
    }

    public static class Memory {
        private int maxHistoryTurns = 50;
        private int maxToolPairsPerTurn = 3;
        private int maxToolArgsPreviewChars = 300;
        private int maxToolResultPreviewChars = 500;
        private int maxHistoryTokens = 24000;
        public int getMaxHistoryTurns() { return maxHistoryTurns; }
        public void setMaxHistoryTurns(int v) { maxHistoryTurns = v; }
        public int getMaxToolPairsPerTurn() { return maxToolPairsPerTurn; }
        public void setMaxToolPairsPerTurn(int v) { maxToolPairsPerTurn = v; }
        public int getMaxToolArgsPreviewChars() { return maxToolArgsPreviewChars; }
        public void setMaxToolArgsPreviewChars(int v) { maxToolArgsPreviewChars = v; }
        public int getMaxToolResultPreviewChars() { return maxToolResultPreviewChars; }
        public void setMaxToolResultPreviewChars(int v) { maxToolResultPreviewChars = v; }
        public int getMaxHistoryTokens() { return maxHistoryTokens; }
        public void setMaxHistoryTokens(int v) { maxHistoryTokens = v; }
    }

    public static class Progress {
        private int maxToolArgsPreviewChars = 1000;
        private int maxToolResultPreviewChars = 2000;
        public int getMaxToolArgsPreviewChars() { return maxToolArgsPreviewChars; }
        public void setMaxToolArgsPreviewChars(int v) { maxToolArgsPreviewChars = v; }
        public int getMaxToolResultPreviewChars() { return maxToolResultPreviewChars; }
        public void setMaxToolResultPreviewChars(int v) { maxToolResultPreviewChars = v; }
    }

    public static class Lock {
        private Duration ttl = Duration.ofMinutes(10);
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration v) { ttl = v; }
    }

    public static class Executor {
        private int runCoreSize = 2;
        private int runMaxSize = 4;
        private int runQueueCapacity = 100;
        private int toolCoreSize = 4;
        private int toolMaxSize = 8;
        private int toolQueueCapacity = 100;
        public int getRunCoreSize() { return runCoreSize; }
        public void setRunCoreSize(int v) { runCoreSize = v; }
        public int getRunMaxSize() { return runMaxSize; }
        public void setRunMaxSize(int v) { runMaxSize = v; }
        public int getRunQueueCapacity() { return runQueueCapacity; }
        public void setRunQueueCapacity(int v) { runQueueCapacity = v; }
        public int getToolCoreSize() { return toolCoreSize; }
        public void setToolCoreSize(int v) { toolCoreSize = v; }
        public int getToolMaxSize() { return toolMaxSize; }
        public void setToolMaxSize(int v) { toolMaxSize = v; }
        public int getToolQueueCapacity() { return toolQueueCapacity; }
        public void setToolQueueCapacity(int v) { toolQueueCapacity = v; }
    }
}
