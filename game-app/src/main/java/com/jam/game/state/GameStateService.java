package com.jam.game.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 游戏世界状态（内存版，最简切片用，后续可替换为数据库持久化）。
 * 维护玩家对各 NPC 的好感度、已获得物品、当前地点，供游戏业务 Tool 读写。
 */
@Service
public class GameStateService {

    private final Map<String, Integer> affection = new ConcurrentHashMap<>();
    private final Map<String, Boolean> inventory = new ConcurrentHashMap<>();
    private final Map<String, String> location = new ConcurrentHashMap<>();

    /** 好感度 += delta，返回最新值。key = playerId + ":" + npcId */
    public int addAffection(String playerId, String npcId, int delta) {
        String key = playerId + ":" + npcId;
        return affection.merge(key, delta, Integer::sum);
    }

    public int getAffection(String playerId, String npcId) {
        return affection.getOrDefault(playerId + ":" + npcId, 0);
    }

    public void grantItem(String playerId, String itemName) {
        inventory.put(playerId + ":" + itemName, Boolean.TRUE);
    }

    public boolean hasItem(String playerId, String itemName) {
        return inventory.getOrDefault(playerId + ":" + itemName, Boolean.FALSE);
    }

    public void setLocation(String playerId, String loc) {
        location.put(playerId, loc);
    }

    public String getLocation(String playerId) {
        return location.get(playerId);
    }
}