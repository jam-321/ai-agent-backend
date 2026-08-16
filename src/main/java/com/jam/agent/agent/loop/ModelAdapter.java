package com.jam.agent.agent.loop;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelAdapter {
    private static final String SYSTEM = "你是一个友好、专业的中文 AI Agent。需要准确时间时调用 current_time，需要精确算术时调用 calculate，需要查询历史工具完整数据时调用 query_conversation_node。不要编造工具结果。";
    private final ChatModel model;
    private final boolean enabled;
    public ModelAdapter(ChatModel model, @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.model = model; this.enabled = apiKey != null && !apiKey.isBlank() && !apiKey.contains("dummy");
    }
    public ModelResult call(List<Message> messages, List<ToolCallback> tools, AgentExecutionContext context) {
        if (!enabled) return new ModelResult(new AssistantMessage("[mock] 尚未配置 DEEPSEEK_API_KEY。你的问题是：" + context.currentQuery()));
        try {
            var options = ToolCallingChatOptions.builder().toolCallbacks(tools).internalToolExecutionEnabled(false)
                    .toolContext(Map.of("userId", context.userId(), "conversationId", context.conversationId(), "turnId", context.turnId(), "traceId", context.traceId())).build();
            List<Message> promptMessages = new java.util.ArrayList<>();
            promptMessages.add(new SystemMessage(SYSTEM));
            promptMessages.addAll(messages);
            ChatResponse response = model.call(new Prompt(promptMessages, options));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) throw new RetryableModelException("模型未返回有效响应。", null);
            return new ModelResult(response.getResult().getOutput());
        } catch (RetryableModelException ex) { throw ex; }
        catch (RuntimeException ex) { throw new RetryableModelException("模型调用失败：" + safe(ex), ex); }
    }
    private String safe(RuntimeException ex) { String s=ex.getMessage(); return s==null ? ex.getClass().getSimpleName() : (s.length()>500?s.substring(0,500):s); }
    public record ModelResult(AssistantMessage message) {}
    public static class RetryableModelException extends RuntimeException { public RetryableModelException(String m, Throwable c) { super(m,c); } }
}
