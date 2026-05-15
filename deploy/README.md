# Deploy

## 文件说明

| 文件 | 用途 |
|---|---|
| `docker-compose.yml` | 仅基础设施（MySQL + Redis + RabbitMQ），不包含业务服务 |
| `docker-compose.app.yml` | 全栈应用编排（基础设施 + backend + frontend） |

## 快速启动

```bash
# 从项目根目录执行
docker compose -f deploy/docker-compose.app.yml up -d --build

# 查看日志
docker compose -f deploy/docker-compose.app.yml logs -f backend

# 停止并清理
docker compose -f deploy/docker-compose.app.yml down
```

## 访问地址

| 服务 | 地址 | 说明 |
|---|---|---|
| Frontend | http://localhost:5173 | Vue 3 控制台 |
| Backend API | http://localhost:8080 | Spring Boot API |
| Actuator Health | http://localhost:8080/actuator/health | 健康检查 |
| RabbitMQ Management | http://localhost:15672 | guest/guest |

## 环境变量

复制 `.env.example` 到 `.env` 并按需修改：

```bash
cp .env.example .env
```

关键变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_PASSWORD` | `platform123` | 数据库密码 |
| `JWT_SECRET` | (占位值) | **生产必须替换为 256-bit 以上强随机值** |
| `MODEL_GATEWAY_PROVIDER` | `MOCK` | 默认模型供应商 |
| `GITHUB_CLIENT_ID` | (空) | GitHub OAuth Client ID |
| `GITHUB_CLIENT_SECRET` | (空) | GitHub OAuth Client Secret |

## 仅启动基础设施

```bash
docker compose -f deploy/docker-compose.yml up -d
```

## 常见问题

### MySQL 连接失败

确认 MySQL 容器已启动且健康检查通过：

```bash
docker compose -f deploy/docker-compose.app.yml ps
```

### Flyway 迁移失败

如果本地库曾出现迁移失败，可进入 MySQL 容器修复：

```bash
docker exec -it ai-coding-platform-mysql mysql -uroot -pplatform123 ai_coding_platform -e "DELETE FROM flyway_schema_history WHERE success = 0;"
```

然后重启 backend 容器。

### 前端 502 / API 代理失败

确认 backend 容器健康，nginx 代理目标为 `http://backend:8080`（Compose 内 DNS）。

### Chat SSE 无输出

nginx 已对 `/api/` 路径关闭 `proxy_buffering` 和 `proxy_cache`，`proxy_read_timeout` 设为 3600s。如仍有问题，检查反向代理链中是否有其他 buffering 层。

### GitHub OAuth redirect URL 不匹配

容器环境中，GitHub OAuth redirect URI 必须与 GitHub App 配置一致。如使用 localhost，确保 `GITHUB_REDIRECT_URI` 指向可访问地址。
