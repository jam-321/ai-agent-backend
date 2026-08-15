# ai-agent-backend

AI Agent 后端服务，技术栈为 Java 17、Spring Boot 3.5 和 Spring AI。

## 本地运行

```bash
mvn spring-boot:run
```

接口：

- `GET /api/ping`：健康检查
- `POST /api/chat`：对话接口

未配置 `DEEPSEEK_API_KEY` 时，`/api/chat` 返回 mock 回复；配置后调用 DeepSeek 兼容接口。

## 中间件

`docker/docker-compose.yml` 包含 MySQL 和 Redis。复制 `docker/.env.example` 为 `docker/.env` 后，可在 `docker/` 目录执行：

```bash
docker compose up -d
```
