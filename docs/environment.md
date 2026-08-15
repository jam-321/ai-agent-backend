# 本机环境记录

> 更新时间：2026-08-15。此文件记录 ai-agent-backend 依赖的本机环境，供后续会话恢复上下文。

## 运行时

- JDK 17：`E:\soft\jdk\jdk-17.0.20.8-hotspot`
- Maven：`E:\soft\mavenAbout\maven\apache-maven-3.6.3\bin\mvn.cmd`
- Maven settings：`E:\soft\mavenAbout\2024_02_18\settings.xml`
- 构建：`mvn -s E:\soft\mavenAbout\2024_02_18\settings.xml -DskipTests package`

## 环境变量

- `DEEPSEEK_API_KEY`：DeepSeek API Key（用户级环境变量，2026-08-15 已设置）。`application.yml` 通过 `${DEEPSEEK_API_KEY:${AI_API_KEY:sk-dummy-not-configured}}` 读取，旧名 `AI_API_KEY` 自动兼容。

## Docker

- Docker Desktop：4.86.0
- Docker Engine：29.7.2
- Docker Compose：v5.3.1
- WSL：2.7.11
- Docker CLI：`C:\Program Files\Docker\Docker\resources\bin\docker.exe`

本机已安装并启用 WSL 与 `VirtualMachinePlatform`。重启电脑后先启动 Docker Desktop，再启动容器。

## MySQL / Redis

编排文件：`docker/docker-compose.yml`

| 服务 | 容器 | 镜像 | 本机端口 | 数据卷 |
| --- | --- | --- | --- | --- |
| MySQL | `ai-agent-mysql` | `mysql:8.4` | `127.0.0.1:3306` | `docker_mysql_data` |
| Redis | `ai-agent-redis` | `redis:7-alpine` | `127.0.0.1:6379` | `docker_redis_data` |

端口只绑定 `127.0.0.1`，不暴露到局域网。账号和密码默认值见 `docker/.env.example`；实际密码应放在未入库的 `docker/.env` 中。

常用命令：

```powershell
cd E:\ai\code\code\ai-agent-backend

# 启动
docker compose -f docker\docker-compose.yml up -d

# 查看状态
docker compose -f docker\docker-compose.yml ps

# 停止但保留数据
docker compose -f docker\docker-compose.yml down
```

## 已完成验证

- MySQL 容器健康检查通过，版本 8.4.11，数据库 `ai_agent` 已创建。
- Redis 容器健康检查通过，`PING` 返回 `PONG`。
- 两个数据卷均已创建，`down` 不会删除数据。
