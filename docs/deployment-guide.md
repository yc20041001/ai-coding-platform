# Deployment Guide

## 1. 本地 Docker Compose 启动

```bash
# 从项目根目录执行
docker compose -f deploy/docker-compose.app.yml up -d --build

# 查看日志
docker compose -f deploy/docker-compose.app.yml logs -f

# 停止服务
docker compose -f deploy/docker-compose.app.yml down

# 停止并删除数据卷
docker compose -f deploy/docker-compose.app.yml down -v
```

## 2. 环境变量说明

所有环境变量通过 `.env` 文件或 shell 环境注入。关键变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://mysql:3306/ai_coding_platform...` | 数据库连接（Compose 内使用 `mysql` 主机名） |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | `platform123` | 数据库密码 |
| `JWT_SECRET` | (占位值) | **生产必须替换为 256-bit 以上强随机值** |
| `MODEL_GATEWAY_PROVIDER` | `MOCK` | AI 模型供应商 |
| `SPRING_PROFILES_ACTIVE` | (空) | Spring 配置文件 |
| `JAVA_OPTS` | `-Xmx512m` | JVM 参数 |
| `GITHUB_CLIENT_ID` | (空) | GitHub OAuth Client ID |
| `GITHUB_CLIENT_SECRET` | (空) | GitHub OAuth Client Secret |

## 3. 镜像构建

### 本地构建

```bash
# 后端
docker build -t ai-coding-platform-backend:local ./backend

# 前端
docker build --build-arg VITE_API_BASE_URL="" -t ai-coding-platform-frontend:local ./frontend

# 或使用脚本
bash scripts/docker-build-local.sh
```

### 构建参数

前端镜像支持 `VITE_API_BASE_URL` 构建参数：

- Compose 部署：默认 `""`（API 路径已含 `/api` 前缀，由 nginx 反向代理）
- 独立部署：可设为后端完整 URL（如 `http://localhost:8080`）

## 4. CI 工作流

| 工作流 | 文件 | 触发 | 说明 |
|---|---|---|---|
| Backend CI | `.github/workflows/backend-ci.yml` | PR / push main | compile → test → package |
| Frontend CI | `.github/workflows/frontend-ci.yml` | PR / push main | npm ci → typecheck → build |
| Docker Build | `.github/workflows/docker-build.yml` | PR / push main / tag v* | 构建镜像，main/tag 推送 GHCR |

### CI 数据库

Backend CI 使用 GitHub Actions MySQL service container，自动创建 `ai_coding_platform_test` 数据库。

测试环境变量在 workflow 中硬编码（不含真实密钥），通过 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD` 注入。

## 5. GHCR 推送说明

Docker Build 工作流在 `main` 分支和 `v*` 标签推送时自动推送镜像到 GitHub Container Registry：

- `ghcr.io/<owner>/ai-coding-platform-backend:latest`（main 分支）
- `ghcr.io/<owner>/ai-coding-platform-backend:<sha>`
- `ghcr.io/<owner>/ai-coding-platform-backend:v1.0.0`（tag 触发）
- `ghcr.io/<owner>/ai-coding-platform-frontend:latest`（main 分支）
- `ghcr.io/<owner>/ai-coding-platform-frontend:<sha>`
- `ghcr.io/<owner>/ai-coding-platform-frontend:v1.0.0`（tag 触发）

PR 仅构建镜像，不推送。使用 `GITHUB_TOKEN` 和 `packages: write` 权限。

## 6. Smoke Test

```bash
# 启动服务后运行
bash scripts/backend-unified-smoke-test.sh

# 或使用 Docker smoke test（自动启动/等待/测试）
bash scripts/docker-smoke-test.sh

# 测试完成后自动清理
bash scripts/docker-smoke-test.sh --down
```

## 7. 常见问题

### MySQL 连接失败

```
CommunicationsException: Communications link failure
```

1. 确认 MySQL 容器正在运行：`docker compose -f deploy/docker-compose.app.yml ps`
2. 确认 `DB_URL` 在 Compose 内使用 `mysql` 主机名（非 `localhost`）
3. 等待 MySQL healthcheck 通过后 backend 才会启动

### Flyway migration failed

```
FlywayException: Validate failed: Migration checksum mismatch
```

1. 进入 MySQL 容器：`docker exec -it ai-coding-platform-mysql mysql -uroot -pplatform123 ai_coding_platform`
2. 清理失败迁移记录：`DELETE FROM flyway_schema_history WHERE success = 0;`
3. 如需完全重建：`DROP DATABASE ai_coding_platform; CREATE DATABASE ai_coding_platform CHARACTER SET utf8mb4;`
4. 重启 backend 容器：`docker compose -f deploy/docker-compose.app.yml restart backend`

### JWT_SECRET 缺失或太短

```
JWT secret too short (minimum 256 bits)
```

设置 `JWT_SECRET` 环境变量为至少 32 字符的强随机值：

```bash
export JWT_SECRET=$(openssl rand -base64 32)
```

### 前端 502 / API 代理失败

1. 确认 backend 容器健康：`curl http://localhost:8080/actuator/health`
2. 确认 nginx 代理目标 `http://backend:8080` 可解析（Compose 内 DNS）
3. 查看 frontend 日志：`docker compose -f deploy/docker-compose.app.yml logs frontend`

### SSE 无输出或被截断

nginx 已对 `/api/` 路径配置：
- `proxy_buffering off`
- `proxy_cache off`
- `proxy_read_timeout 3600s`

如仍有问题，检查：
1. 反向代理链中是否有 Cloudflare/CDN/其他网关导致 buffering
2. 客户端是否支持 EventSource API

### GitHub OAuth redirect URL 不匹配

容器环境中 GitHub OAuth redirect URI 指向 localhost，需确保 GitHub App 配置中包含该 URI。

### Docker 构建缓慢

- CI 中使用 GitHub Actions cache (`type=gha`) 缓存 Maven 依赖和 npm 缓存
- 本地构建使用 Docker layer caching（pom.xml 和 package.json 先于源码复制）
