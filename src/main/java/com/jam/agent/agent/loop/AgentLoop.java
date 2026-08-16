package com.jam.agent.agent.loop;

import com.jam.agent.agent.event.EventPublisher;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.tool.ToolExecutor;
import com.jam.agent.agent.tool.ToolExecutor.ToolResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AgentLoop {
    private final ModelAdapter model;
    private final ToolExecutor tools;
    private final EventPublisher events;
    private final Executor toolExecutor;
    public AgentLoop(ModelAdapter model, ToolExecutor tools, EventPublisher events,
                     @Qualifier("agentToolExecutor") Executor agentToolExecutor) {
        this.model=model; this.tools=tools; this.events=events; this.toolExecutor=agentToolExecutor;
    }
    public String run(AgentExecutionContext context, int attemptNo, List<Message> history) {
        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(context.currentQuery()));
        int degenerate = 0; String previousSignature = null; int sameSignature = 0;
        List<ToolCallback> callbacks = new ArrayList<>(tools.callbacks().values());
        for (int round=1; round<=context.maxToolRounds(); round++) {
            context.checkDeadline(); events.lifecycle(context, attemptNo, round, "round_start");
            AssistantMessage response = model.call(messages, callbacks, context).message();
            if (response.hasToolCalls()) {
                if (response.getToolCalls().size() > context.maxToolsPerRound()) throw new IllegalStateException("单轮工具调用数量超过上限。");
                messages.add(response);
                if (meaningful(response.getText())) events.assistant(context, attemptNo, round, response.getText());
                List<AssistantMessage.ToolCall> calls=response.getToolCalls();
                List<CompletableFuture<ToolResult>> futures=new ArrayList<>();
                for(int i=0;i<calls.size();i++) { var call=calls.get(i); events.toolStart(context, attemptNo, round, i, call.name(), call.id(), call.arguments()); }
                for(int i=0;i<calls.size();i++) { var call=calls.get(i); int callIndex=i; int roundNo=round; futures.add(CompletableFuture.supplyAsync(() -> tools.execute(context, attemptNo, roundNo, callIndex, call), toolExecutor)); }
                List<ToolResponseMessage.ToolResponse> responses=new ArrayList<>();
                for(int i=0;i<futures.size();i++) { ToolResult result=futures.get(i).join(); responses.add(new ToolResponseMessage.ToolResponse(result.id(), result.name(), result.responseData())); String signature=calls.get(i).name()+":"+(calls.get(i).arguments()==null?"{}":calls.get(i).arguments().replaceAll("\\s+","")); if(signature.equals(previousSignature)) sameSignature++; else {previousSignature=signature;sameSignature=1;} }
                if (sameSignature >= context.maxSameToolSignature()) throw new IllegalStateException("检测到工具重复调用。");
                messages.add(ToolResponseMessage.builder().responses(responses).build()); degenerate=0; continue;
            }
            String text=response.getText();
            if (!meaningful(text)) {
                if (degenerate < context.maxDegenerateRetries()) { degenerate++; messages.add(new UserMessage("上一轮未生成有效正文且没有调用工具。若需要信息，请调用可用工具；否则请直接给出最终答案。")); continue; }
                throw new IllegalStateException("模型连续未生成有效回答。");
            }
            messages.add(response); return text;
        }
        throw new IllegalStateException("工具循环超过 40 轮上限。");
    }
    private boolean meaningful(String text) { return text != null && !text.isBlank() && !text.trim().matches("[.。]+" ); }
}
