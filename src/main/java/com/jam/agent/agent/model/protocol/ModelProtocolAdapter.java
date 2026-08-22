package com.jam.agent.agent.model.protocol;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

/** 把一种模型线协议适配为 AgentLoop 使用的统一消息契约。 */
public interface ModelProtocolAdapter {

    String protocolType();

    ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context);
}
