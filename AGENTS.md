# ai-agent-backend 项目约定

- 后端使用 JDK 17 与 Spring Boot 3.5，不用全局 JDK 8。
- Docker 中间件通过 `docker/docker-compose.yml` 启动，端口只允许绑定到 `127.0.0.1`。

## 知识索引树

| 节点名称 | 是否为叶子节点 | 节点地址 | 说明用途 | 何时注入上下文 |
| --- | --- | --- | --- | --- |
| 设计稿 | 是 | `E:\ai\code\code\ai-agent-backend\docs\design.md` | 主线任务前后端设计文档。 | 需要理解架构、技术选型、里程碑或准备改造设计时读取。 |
| 环境信息 | 是 | `E:\ai\code\code\ai-agent-backend\docs\environment.md` | 主线任务前后端、Docker、MySQL、Redis 等本机环境配置信息。 | 需要构建、启动、连接中间件或排查环境问题时读取。 |
| 里程碑 | 是 | `E:\ai\code\code\ai-agent-backend\docs\里程碑.md` | 主线任务当前阶段和关键成果的摘要，优先于迭代历史注入。 | 恢复主线上下文、判断当前进度或规划下一步时先读取。 |
| 迭代历史 | 否 | `E:\ai\code\code\ai-agent-backend\docs\迭代历史` | 记录主线开发流程；当主线有显著推进时，主动询问用户是否记入迭代历史。 | 需要回顾决策、恢复历史上下文或准备追加迭代记录时读取目录内相关文档。 |

迭代历史采用多个 Markdown 文件分段：默认按日期或显著里程碑创建；单文件过长或进入新阶段时新开文件，不在一个文件里无限追加。
