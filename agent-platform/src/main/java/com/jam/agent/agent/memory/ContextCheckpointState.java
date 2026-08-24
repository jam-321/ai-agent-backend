package com.jam.agent.agent.memory;

import com.jam.agent.agent.model.protocol.ModelCallResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

/** 保存当前 Turn 最近一次压缩后的消息快照，供 Turn 完成时更新持久化检查点。 */
public final class ContextCheckpointState {

    private List<Message> messages = List.of();
    private ModelCallResult usage;

    public synchronized void capture(List<Message> messages, ModelCallResult usage) {
        this.messages = new ArrayList<>(messages);
        this.usage = usage;
    }

    public synchronized boolean exists() {
        return !messages.isEmpty();
    }

    public synchronized List<Message> messages() {
        return List.copyOf(messages);
    }

    public synchronized ModelCallResult usage() {
        return usage;
    }
}
