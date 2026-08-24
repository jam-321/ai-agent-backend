package com.jam.agent.agent.plugin;

import com.jam.agent.agent.event.AgentEvent;
import com.jam.agent.agent.event.Plugin;
import com.jam.agent.agent.event.PluginSubscribes;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;

/** System plugin that projects Agent events into conversation_node rows. */
@PluginSubscribes(
        id = "node_trace",
        events = {"lifecycle", "tool_call", "tool_result", "workflow_step_start",
                "workflow_step_end", "model_call_start", "model_call_end",
                "assistant", "generate"},
        order = 5,
        system = true)
public class NodeTracePlugin implements Plugin {

    private final ConversationNodeRepository nodes;

    public NodeTracePlugin(ConversationNodeRepository nodes) {
        this.nodes = nodes;
    }

    @Override
    public AgentEvent execute(AgentEvent event) {
        String nodeId;
        String nodeName;
        String aggregationKey = event.toolCallId();
        String type;
        String status;

        switch (event.name()) {
            case "lifecycle" -> {
                nodeId = "lifecycle";
                nodeName = "执行状态";
                type = "LIFECYCLE";
                status = "INFO";
                aggregationKey = null;
            }
            case "tool_call", "tool_result" -> {
                nodeId = event.toolName();
                nodeName = event.toolName();
                type = "TOOL_CALL";
                status = "tool_call".equals(event.name())
                        ? "START"
                        : event.error() ? "ERROR" : "SUCCESS";
            }
            case "workflow_step_start", "workflow_step_end" -> {
                nodeId = event.toolName();
                nodeName = event.toolName();
                type = "WORKFLOW_STEP";
                status = "workflow_step_start".equals(event.name())
                        ? "START"
                        : event.error() ? "ERROR" : "SUCCESS";
            }
            case "model_call_start", "model_call_end" -> {
                nodeId = "model_call";
                nodeName = "模型调用";
                type = "MODEL_CALL";
                status = "model_call_start".equals(event.name())
                        ? "START"
                        : event.error() ? "ERROR" : "SUCCESS";
            }
            case "assistant" -> {
                nodeId = "assistant_reply";
                nodeName = "助手回复";
                type = "ASSISTANT_REPLY";
                status = "SUCCESS";
                aggregationKey = null;
            }
            case "generate" -> {
                nodeId = "generate";
                nodeName = "最终回答";
                type = "GENERATE";
                status = event.error() ? "ERROR" : "COMPLETE";
                aggregationKey = null;
            }
            default -> throw new IllegalArgumentException("Unsupported Agent event: " + event.name());
        }

        nodes.insert(
                event.execution().conversationId(),
                event.execution().turnId(),
                event.execution().traceId(),
                event.attemptNo(),
                event.roundNo(),
                event.callIndex(),
                nodeId,
                nodeName,
                aggregationKey,
                type,
                status,
                event.content());
        return event;
    }
}
