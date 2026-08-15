# ai-agent-backend 项目约定

- 后端使用 JDK 17 与 Spring Boot 3.5，不用全局 JDK 8。
- 本机 JDK、Maven、Docker、MySQL、Redis 的路径和状态记录见 `docs/environment.md`。
- Docker 中间件通过 `docker/docker-compose.yml` 启动，端口只允许绑定到 `127.0.0.1`。
