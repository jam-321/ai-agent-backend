package com.jam.game.tool;

import com.jam.agent.agent.tool.definition.AgentToolProvider;
import com.jam.game.state.GameStateService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 游戏业务 Tool：让 agentLoop 能读写游戏世界状态。
 * 实现 AgentToolProvider 后，会被 agent-platform 的 ToolRegistry 自动发现并注入 agent。
 */
@Component
public class GameTools implements AgentToolProvider {

    private final GameStateService state;

    public GameTools(GameStateService state) {
        this.state = state;
    }

    @Tool(name = "game_affection", description = "改变你和某个角色的好感度（正面互动加分，负面扣分）。返回最新好感度。")
    public String gameAffection(
            @ToolParam(description = "当前玩家编号，一般由系统注入") String playerId,
            @ToolParam(description = "角色编号") String npcId,
            @ToolParam(description = "好感度变化量，正值加分负值扣分") int delta,
            @ToolParam(description = "变化原因，用于记录/回放") String reason) {
        int value = state.addAffection(playerId, npcId, delta);
        return "好感度变化(" + reason + ")，与角色[" + npcId + "]好感度现为 " + value + "。";
    }

    @Tool(name = "game_grant_item", description = "获得一件物品（如武器、线索、纪念品）。")
    public String gameGrantItem(
            @ToolParam(description = "当前玩家编号") String playerId,
            @ToolParam(description = "物品名称") String itemName) {
        state.grantItem(playerId, itemName);
        return "获得物品：" + itemName + "。";
    }

    @Tool(name = "game_travel", description = "移动到一个地点，并推进一段剧情。")
    public String gameTravel(
            @ToolParam(description = "当前玩家编号") String playerId,
            @ToolParam(description = "目的地地点名") String location) {
        state.setLocation(playerId, location);
        return "你来到：" + location + "。";
    }
}