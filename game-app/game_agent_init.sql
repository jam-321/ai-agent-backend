DELETE FROM agent_config WHERE agent_key = 'game_kandou';
INSERT INTO agent_config
  (agent_key, admin_only, execution_type, system_prompt, enabled_plugins, enabled_tools, magic_params, image_history_mode, model_provider_key, model_name, model_temperature, created_at, updated_at)
VALUES
  ('game_kandou', 0, 'LOOP',
   '你是《寻找小光》这款游戏里的看板娘助手，名字叫小光。你了解这个游戏的一切（作者、世界观、角色、主线）。你的职责是：陪伴玩家、推进剧情、记录并改变游戏世界状态。当玩家做出影响剧情走向的行为时（比如增进与某个角色的关系、获得物品、移动到新地点），主动调用 game_affection / game_grant_item / game_travel 来真正改变游戏世界。说话亲切、有角色代入感，但始终服务于推进游戏体验。',
   '[]',
   '["game_affection","game_grant_item","game_travel","current_time"]',
   '{"budget": {"maxOutputTokens": 8192, "maxContextTokens": 200000, "maxTokensPerTurn": 2000000, "maxUserInputTokens": 32000, "safetyMarginTokens": 4096}, "memory": {"keepRecentTokens": 30000, "compactionEnabled": true, "maxToolPairsPerTurn": 3, "maxToolResultTokens": 5000, "compactionTriggerTokens": 160000, "compactedToolPreviewChars": 1200}}',
   'FULL_IMAGE_HISTORY', 'deepseek', 'deepseek-v4-flash', 0.7, NOW(), NOW());