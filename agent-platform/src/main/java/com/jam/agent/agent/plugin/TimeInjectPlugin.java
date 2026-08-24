package com.jam.agent.agent.plugin;

import com.jam.agent.agent.event.AgentEvent;
import com.jam.agent.agent.event.AgentTurnContext;
import com.jam.agent.agent.event.Plugin;
import com.jam.agent.agent.event.PluginSubscribes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/** Optional example plugin that injects the current time at turn_start. */
@PluginSubscribes(id = "time_inject", events = "turn_start", order = 10)
public class TimeInjectPlugin implements Plugin {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public AgentEvent execute(AgentEvent event) {
        AgentTurnContext turn = event.turn();
        if (turn == null) {
            return event;
        }
        for (int index = turn.messages().size() - 1; index >= 0; index--) {
            Message message = turn.messages().get(index);
            if (message instanceof UserMessage user) {
                String text = user.getText() == null ? "" : user.getText();
                turn.messages().set(index, new UserMessage(
                        text + "（当前时间：" + LocalDateTime.now().format(FORMATTER) + "）"));
                break;
            }
        }
        return event;
    }
}
