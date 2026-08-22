package com.jam.agent.agent.service;

import com.jam.agent.agent.loop.ModelAdapter;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** 在主回答完成后异步生成图片摘要；摘要失败不影响主 Turn 的成功状态。 */
@Service
public class ImageSummaryService {

    private final ImageAttachmentService images;
    private final ModelAdapter model;
    private final ConversationNodeRepository nodes;
    private final Executor executor;

    public ImageSummaryService(
            ImageAttachmentService images,
            ModelAdapter model,
            ConversationNodeRepository nodes,
            @Qualifier("agentToolExecutor") Executor executor) {
        this.images = images;
        this.model = model;
        this.nodes = nodes;
        this.executor = executor;
    }

    public void submit(AgentExecutionContext context) {
        if (context.attachmentIds().isEmpty() || !context.modelConfig().supportsImageInput()) return;
        for (Long assetId : context.attachmentIds()) {
            executor.execute(() -> summarizeOne(context, assetId));
        }
    }

    private void summarizeOne(AgentExecutionContext context, long assetId) {
        String key = context.traceId() + ":image-summary:" + assetId;
        writeNode(context, key, assetId, "START", "开始生成图片摘要");
        try {
            List<Message> messages = List.of(UserMessage.builder()
                    .text("请用简洁中文描述这张图片的可观察内容，重点说明文字、界面元素、人物、物品、数字和布局。只输出摘要正文，不要输出 JSON、标题或额外解释。")
                    .media(images.toMedia(context.userId(), assetId))
                    .build());
            String summary = model.call(messages, List.of(), context).message().getText();
            if (summary == null || summary.isBlank()) throw new IllegalStateException("模型未生成图片摘要。");
            images.saveSummary(assetId, summary, context.modelConfig().modelName());
            writeNode(context, key, assetId, "SUCCESS", summary);
        } catch (Exception exception) {
            writeNode(context, key, assetId, "ERROR", safeMessage(exception));
        }
    }

    private void writeNode(AgentExecutionContext context, String key, long assetId, String status, String content) {
        nodes.insert(context.conversationId(), context.turnId(), context.traceId(), 1, null, null,
                "image-summary", "图片摘要", key, "GENERATE", status, content, assetId);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
