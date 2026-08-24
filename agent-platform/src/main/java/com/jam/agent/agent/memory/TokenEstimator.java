package com.jam.agent.agent.memory;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/** 调用前的保守估算器；真实预算结算仍以供应商返回的 usage 为准。 */
@Component
public class TokenEstimator {

    private static final int MESSAGE_OVERHEAD = 8;
    private static final int IMAGE_ESTIMATE = 1200;

    public int estimate(List<Message> messages, List<ToolCallback> tools) {
        long tokens = 0;
        for (Message message : messages) {
            tokens += MESSAGE_OVERHEAD + estimateText(message.getText());
            if (message instanceof UserMessage user && user.getMedia() != null) {
                tokens += (long) user.getMedia().size() * IMAGE_ESTIMATE;
            }
            if (message instanceof AssistantMessage assistant) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    tokens += estimateText(call.name()) + estimateText(call.arguments()) + 12;
                }
            }
            if (message instanceof ToolResponseMessage responses) {
                for (ToolResponseMessage.ToolResponse response : responses.getResponses()) {
                    tokens += estimateText(response.name()) + estimateText(response.responseData()) + 12;
                }
            }
        }
        for (ToolCallback tool : tools) {
            tokens += estimateText(tool.getToolDefinition().name());
            tokens += estimateText(tool.getToolDefinition().description());
            tokens += estimateText(tool.getToolDefinition().inputSchema());
        }
        return (int) Math.min(Integer.MAX_VALUE, tokens);
    }

    public int estimateText(String value) {
        if (value == null || value.isBlank()) return 0;
        int cjk = 0;
        int other = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                cjk++;
            } else {
                other++;
            }
        }
        return cjk + Math.max(1, (other + 3) / 4);
    }
}
