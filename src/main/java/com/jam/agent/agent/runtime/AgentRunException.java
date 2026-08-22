package com.jam.agent.agent.runtime;

public class AgentRunException extends RuntimeException {
    private final boolean retryable;

    public AgentRunException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AgentRunException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
