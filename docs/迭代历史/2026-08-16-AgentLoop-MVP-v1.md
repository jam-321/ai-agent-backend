# AgentLoop MVP v1

> 日期：2026-08-16  
> 分支：前后端均为 `feature/agent-loop`

## 本次目标

把原来的同步单轮聊天升级为可用 Agent：支持跨轮短期记忆、手动 Tool Calling、工具异常自修复、过程可视化和服务重启后的会话恢复。

## 数据模型

- `conversation`：用户可见会话，按 `user_id + updated_at` 查询，支持软删除。
- `conversation_turn`：只存 user/assistant 终态；运行中允许暂时只有 user，完成后每轮恰好两行。
- `conversation_node`：append-only 过程事件，保存 `trace_id / attempt_no / round_no / call_index / aggr_key` 和完整 Tool 内容。
- Tool START 与 SUCCESS/ERROR 共用模型提供的 Tool Call ID；`round_no + call_index` 保留模型原始顺序。

## 循环设计

- POST `/api/chat` 事务创建 user Turn 后返回 HTTP 202，Agent 在有界线程池异步执行。
- OuterLoop 最多 2 次，只重试模型调用异常；Spring AI 内部重试降为 1 次，避免双层指数退避。
- InnerLoop 最多 40 个模型 Round，关闭框架自动 Tool 执行。
- 同轮多个 Tool 先全部写 START，再并行执行；完成事件可以乱序，回注模型时按 `call_index` 恢复原序。
- Tool 普通异常包装为 `success=false, is_error=true` 的 JSON 结果并继续循环。
- 空白或只有句号的退化回复最多 nudge 两次，之后进入 ERROR；重复工具签名达到阈值时终止。

## 记忆与事件流

- 每个 Run 从 MySQL 重建最近 50 个完整历史 Turn，当前 query 只在内存末尾追加一次。
- 每轮最多回填 3 对完整 Tool 消息；超长入参/结果改写为合法 JSON 预览，可由 `query_conversation_node` 查询全文。
- `EventPublisher` 映射 LIFECYCLE、TOOL_CALL、ASSISTANT_REPLY、GENERATE 四类 Node。
- `TurnFinalizer` 在同一事务写 assistant 终态、GENERATE 终态和会话更新时间。
- 启动时扫描 user 已写但 assistant 缺失的残留 Turn，幂等补为 ERROR。

## 前端

- 增加会话侧栏、新建、切换、软删除和历史消息加载。
- 每 1.5 秒轮询 progress，展示助手阶段消息及 Tool START/SUCCESS/ERROR 卡片。
- COMPLETE/ERROR 后停止轮询；切换会话时清理旧轮询，返回未完成会话时恢复轮询。
- 修正未登录 `/api/auth/me` 为 401，避免登录页误报后端不可用。

## 验证结果

- 后端 Maven package 成功；计算器 JUnit 2/2 通过。
- 前端 Vite production build 成功，登录页桌面与 390px 移动视口检查正常。
- 真实 DeepSeek 调用 `current_time` 后回答当前时间和星期。
- `calculate` 精确得到 `345678 * 912345 = 315377594910`。
- 下一轮基于历史结果调用计算工具，得到 `630755189820`。
- MySQL 检查三个完成 Turn 均恰好两条消息，Tool START/终态配对正确。
- 用户 A 访问用户 B 会话返回 404；同一会话并发提交返回 409。
- MySQL、Redis 容器健康；后端运行于 8080，前端运行于 8081。
