package com.jam.agent.agent.event;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.repository.ConversationNodeRepository;
import org.springframework.stereotype.Component;

/** Persists every Agent state transition as a new conversation_node row. */
@Component
public class DatabaseEventPublisher implements EventPublisher {

    private final ConversationNodeRepository nodes;

    public DatabaseEventPublisher(ConversationNodeRepository nodes) {
        this.nodes = nodes;
    }

    @Override
    public void lifecycle(
            AgentExecutionContext context,
            int attemptNo,
            Integer roundNo,
            String content) {
        insert(
                context,
                attemptNo,
                roundNo,
                null,
                "lifecycle",
                "执行状态",
                null,
                "LIFECYCLE",
                "INFO",
                content);
    }

    @Override
    public void toolStart(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            int callIndex,
            String toolName,
            String toolCallId,
            String arguments) {
        insert(
                context,
                attemptNo,
                roundNo,
                callIndex,
                toolName,
                toolName,
                toolCallId,
                "TOOL_CALL",
                "START",
                arguments);
    }

    @Override
    public void toolEnd(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            int callIndex,
            String toolName,
            String toolCallId,
            String result,
            boolean error) {
        insert(
                context,
                attemptNo,
                roundNo,
                callIndex,
                toolName,
                toolName,
                toolCallId,
                "TOOL_CALL",
                error ? "ERROR" : "SUCCESS",
                result);
    }

    @Override
    public void assistant(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            String content) {
        insert(
                context,
                attemptNo,
                roundNo,
                null,
                "assistant_reply",
                "助手回复",
                null,
                "ASSISTANT_REPLY",
                "SUCCESS",
                content);
    }

    @Override
    public void generate(
            AgentExecutionContext context,
            int attemptNo,
            String content,
            boolean error) {
        insert(
                context,
                attemptNo,
                null,
                null,
                "generate",
                "最终回答",
                null,
                "GENERATE",
                error ? "ERROR" : "COMPLETE",
                content);
    }

    private void insert(
            AgentExecutionContext context,
            int attemptNo,
            Integer roundNo,
            Integer callIndex,
            String nodeId,
            String nodeName,
            String aggregationKey,
            String type,
            String status,
            String content) {
        nodes.insert(
                context.conversationId(),
                context.turnId(),
                context.traceId(),
                attemptNo,
                roundNo,
                callIndex,
                nodeId,
                nodeName,
                aggregationKey,
                type,
                status,
                content);
    }
}
