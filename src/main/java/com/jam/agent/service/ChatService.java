package com.jam.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 对话服务：封装 Spring AI ChatClient 调用 LLM。
 *
 * <p>未配置 DEEPSEEK_API_KEY 时返回 mock 回复，方便前后端先联调，
 * 配置后自动切换为真实 LLM 调用。</p>
 */
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = "你是一个友好、专业的 AI 助手，请用简洁清晰的中文回答用户问题。";

    private final ChatClient chatClient;
    private final boolean llmEnabled;

    public ChatService(ChatModel chatModel,
                       @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.llmEnabled = apiKey != null && !apiKey.isBlank() && !apiKey.contains("dummy");
    }

    public String chat(String message) {
        if (message == null || message.isBlank()) {
            return "请输入消息内容。";
        }
        if (!llmEnabled) {
            return "[mock] 尚未配置 DEEPSEEK_API_KEY，当前返回模拟回复。\n你刚才说：" + message;
        }
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }
}
