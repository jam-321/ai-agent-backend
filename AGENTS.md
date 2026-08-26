# ai-agent-backend 项目约定

- 后端使用 JDK 17 与 Spring Boot 3.5，不用全局 JDK 8。
- Docker 中间件通过 `docker/docker-compose.yml` 启动，端口只允许绑定到 `127.0.0.1`。

## 本地服务启动约定

- `8080`（后端）和 `8081`（前端）默认留给用户通过 IDEA 或本地终端手动启动。
- Codex 为构建、联调或验收临时启动服务时，优先使用 `18080`（后端）和 `18081`（前端），避免占用用户开发端口。
- Codex 启动的临时前后端服务应在验证结束后主动关闭，不长期留在后台运行。
- 每次完成涉及运行验证的任务时，明确报告前端、后端、MySQL、Redis 的运行状态及监听端口。
- MySQL 和 Redis 可通过 Docker 长期运行；除非用户明确要求，不因结束前后端验证而停止中间件。
- 关闭服务前先核对监听端口、进程 ID 和进程归属，避免终止用户从 IDEA 或其他终端启动的进程。

## 后端包结构约定

- 顶层按功能模块组织，当前核心模块为 `agent`、`auth`、`conversation`、`monitoring` 和 `common`。
- 每个功能模块内部按实际复杂度使用 `controller`、`service`、`dto`、`persistence` 等分层；不再新增全局 `controller/service/mapper` 横向包。
- `entity`、`mapper`、`repository` 跟随数据所属模块放入 `persistence`，业务代码不直接跨模块调用其他模块的 Mapper 或 Entity。
- RAG、Skill 等未来能力作为独立功能模块；Milvus、Redis、模型供应商等技术实现放在所属功能模块的基础设施层，不与业务能力平级。
- 小模块不强制创建空分层目录，类增多后再按职责拆分；通用代码只有被多个模块稳定复用时才进入 `common`。

## 知识索引树

| 节点名称 | 是否为叶子节点 | 节点地址 | 说明用途 | 何时注入上下文 |
| --- | --- | --- | --- | --- |
| 状态 | 是 | `E:\ai\code\code\ai-agent-backend\docs\状态.md` | 主线当前真相：进度、最近决策、下一步、关键约束，最前沿。 | 每次恢复主线上下文、判断当前进度、规划下一步时**最先**读取。 |
| 文档索引 | 是 | `E:\ai\code\code\ai-agent-backend\docs\index.md` | docs 目录导航，说明各文档作用与读取优先级。 | 首次进入主线、需要定位 docs 中"当前真相/设计/环境/历史"对应文档时读取。 |
| 架构参考 | 是 | `E:\ai\code\code\ai-agent-backend\docs\迭代历史\AI-Agent方案设计.md` | 早期"从零开发通用 Agent"的架构设计，已过时，作历史/参考。 | 回顾早期架构设计、准备远期四阶段回补时读取。 |
| 环境信息 | 是 | `E:\ai\code\code\ai-agent-backend\docs\environment.md` | 主线任务前后端、Docker、MySQL、Redis 等本机环境配置信息。 | 需要构建、启动、连接中间件或排查环境问题时读取。 |
| 里程碑 | 是 | `E:\ai\code\code\ai-agent-backend\docs\里程碑.md` | 阶段成果归档：已达成能力与验证结果，相对稳定。 | 读过 `状态.md` 后，回顾已完成能力、确认已具备的功能时读取。 |
| 未来规划 | 是 | `E:\ai\code\code\ai-agent-backend\docs\未来规划.md` | 主线后续阶段规划（M4 上线 + 仿 dsh 的四阶段改造路线）与现状差距。 | 规划下一步、评估改造方向、准备实施阶段 1~4 时读取。 |
| 迭代历史 | 否 | `E:\ai\code\code\ai-agent-backend\docs\迭代历史` | 记录主线开发流程；当主线有显著推进时，主动询问用户是否记入迭代历史。 | 需要回顾决策、恢复历史上下文或准备追加迭代记录时读取目录内相关文档。 |

迭代历史采用多个 Markdown 文件分段：默认按日期或显著里程碑创建；单文件过长或进入新阶段时新开文件，不在一个文件里无限追加。
