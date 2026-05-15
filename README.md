# AI Coding Platform

Enterprise AI Coding Collaboration Platform — 企业级智能编程协作平台。

## 技术栈

| 组件 | 版本/技术 |
|------|----------|
| Java | 17 |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.7 |
| Flyway | 10.20.1 |
| MySQL | 8.0 |
| Redis | 7 |
| RabbitMQ | 3 (management) |
| Maven | 3.x |
| JWT | jjwt 0.12.x |
| Spring Security | 6.x |

## 项目结构

```
ai-coding-platform/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/aicoding/platform/
│       ├── agent/              # Agent 模块
│       ├── audit/              # 审计日志模块
│       ├── auth/               # 认证授权模块
│       ├── chat/               # Chat 会话/消息/SSE 模块
│       ├── common/             # 公共组件 (异常/分页/响应)
│       ├── member/             # 项目成员/权限模块
│       ├── modelgateway/       # 模型网关模块
│       ├── observability/      # 可观测性模块 (用量/概览)
│       ├── orchestrator/       # Agent 编排器模块
│       ├── project/            # 项目模块
│       ├── rag/                # RAG 知识库模块
│       ├── repository/         # 代码仓库模块
│       ├── security/           # 安全模块 (JWT/Filter)
│       └── task/               # 任务模块
├── frontend/                   # Vue 3 企业级控制台
│   └── src/modules/
│       ├── admin/              # Observability / 审计日志
│       ├── agent/              # Agent 管理
│       ├── auth/               # 登录 / Auth Store
│       ├── chat/               # Chat 会话 (SSE)
│       ├── dashboard/          # 首页 Dashboard
│       ├── knowledge/          # 知识库管理
│       ├── member/             # 成员管理
│       ├── project/            # 项目管理
│       ├── repository/         # 仓库管理
│       └── task/               # 任务管理 / 执行详情
├── deploy/                     # 部署配置
│   └── docker-compose.yml      # 本地 Docker Compose 环境
├── docs/                       # 实施文档
├── scripts/                    # 工具脚本
├── .env.example                # 环境变量模板
└── README.md                   # 本文件
```

## 本地依赖

启动前需要以下基础设施：

- MySQL 8.0 (端口 3307)
- Redis 7 (端口 6379)
- RabbitMQ 3 (端口 5672, Management 15672)

## Docker Compose 启动

```bash
# 启动所有基础设施
docker compose -f deploy/docker-compose.yml up -d

# 检查服务状态
docker compose -f deploy/docker-compose.yml ps

# 停止服务
docker compose -f deploy/docker-compose.yml down
```

## 环境变量配置

```bash
# 复制环境变量模板
cp .env.example .env

# 按需修改配置
source .env
```

关键环境变量说明：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DB_URL | 数据库连接 | jdbc:mysql://127.0.0.1:3307/ai_coding_platform |
| DB_USERNAME | 数据库用户 | root |
| DB_PASSWORD | 数据库密码 | platform123 |
| JWT_SECRET | JWT 签名密钥 | (至少 256 位) |
| MODEL_GATEWAY_PROVIDER | 默认模型供应商 | MOCK |
| MODEL_GATEWAY_TIMEOUT_MS | 请求超时 (ms) | 60000 |
| MODEL_GATEWAY_RETRY_TIMES | 重试次数 | 1 |
| OPENAI_ENABLED | 启用 OpenAI | false |
| OPENAI_API_KEY | OpenAI API Key | (需自行配置) |
| CLAUDE_ENABLED | 启用 Claude | false |
| CLAUDE_API_KEY | Claude API Key | (需自行配置) |
| DEEPSEEK_ENABLED | 启用 DeepSeek | false |
| DEEPSEEK_API_KEY | DeepSeek API Key | (需自行配置) |
| QWEN_ENABLED | 启用 Qwen | false |
| QWEN_API_KEY | Qwen API Key | (需自行配置) |
| GEMINI_ENABLED | 启用 Gemini | false |
| GEMINI_API_KEY | Gemini API Key | (需自行配置) |

## 后端启动

```bash
# 编译
cd backend
mvn clean compile

# 运行测试
mvn test

# 启动应用（确保 MySQL 已运行）
source ../.env
mvn spring-boot:run
```

启动后验证：

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info

# 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'
```

## 前端启动

```bash
cd frontend
npm install
cp .env.example .env
npm run dev -- --host 0.0.0.0
```

浏览器访问 `http://localhost:5173`。

默认登录账号：

```text
邮箱：admin@example.com
密码：Admin@123456
```

前端详细文档见 [frontend/README.md](frontend/README.md)。

## 测试

### 测试分层

| 层级 | 技术 | 命令 |
|------|------|------|
| 后端集成测试 | JUnit 5 + Spring Boot Test | `cd backend && mvn test` |
| 前端 E2E | Playwright | `cd frontend && npm run test:e2e` |
| Smoke Test | Shell (curl) | `bash scripts/backend-unified-smoke-test.sh` |

### 后端集成测试

```bash
# 创建测试数据库
mysql -u root -e "CREATE DATABASE IF NOT EXISTS ai_coding_platform_test CHARACTER SET utf8mb4"

# 运行测试
cd backend
mvn test
```

测试使用独立数据库 `ai_coding_platform_test`，不污染开发库。默认使用 Mock 模型网关，不依赖真实 API Key。

### 前端 E2E 测试

```bash
# 安装 Playwright（首次）
cd frontend
npm install -D @playwright/test
npx playwright install chromium

# 启动后端（另一个终端）
cd backend && mvn spring-boot:run

# 运行 E2E 测试
npm run test:e2e
```

### 一键检查脚本

```bash
# 后端编译 + 测试 + 打包
bash scripts/run-backend-checks.sh

# 前端类型检查 + 构建 + E2E
bash scripts/run-frontend-checks.sh

# 全部检查
bash scripts/run-all-checks.sh
```

### 演示数据

```bash
# 初始化演示数据（需要后端已启动）
bash scripts/dev-seed-demo-data.sh

# 重置数据库（仅允许本地开发库/测试库）
bash scripts/dev-reset-db.sh --yes
```

详见 [docs/demo-data-guide.md](docs/demo-data-guide.md)。

### Smoke Test

统一回归冒烟测试脚本：

```bash
# 确保应用已启动
bash scripts/backend-unified-smoke-test.sh
```

默认使用 Mock Provider，不依赖真实模型 API Key。

测试覆盖：
- 健康检查
- 登录认证
- 项目管理
- Agent 管理
- 知识库 RAG 上传/搜索
- Chat 会话/消息/SSE 流式输出
- Task 创建/执行
- 模型网关调用
- 无认证拦截
- 重复执行冲突检测

前端冒烟测试详见 [docs/frontend-smoke-test-plan.md](docs/frontend-smoke-test-plan.md)。

测试策略详见 [docs/testing-strategy.md](docs/testing-strategy.md)。

## Docker 部署

### 全栈一键启动

```bash
# 构建镜像并启动所有服务（MySQL + Redis + RabbitMQ + Backend + Frontend）
docker compose -f deploy/docker-compose.app.yml up -d --build

# 查看日志
docker compose -f deploy/docker-compose.app.yml logs -f backend

# 停止
docker compose -f deploy/docker-compose.app.yml down
```

启动后访问：

| 服务 | 地址 |
|---|---|
| 前端控制台 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| 健康检查 | http://localhost:8080/actuator/health |
| RabbitMQ Management | http://localhost:15672 |

### 单独构建镜像

```bash
# 后端
docker build -t ai-coding-platform-backend:local ./backend

# 前端
docker build --build-arg VITE_API_BASE_URL="" -t ai-coding-platform-frontend:local ./frontend

# 或使用脚本
bash scripts/docker-build-local.sh
```

### 环境变量

Compose 启动时可通过 `.env` 文件覆盖关键变量：

```bash
cp .env.example .env
```

**生产环境必须替换 `JWT_SECRET`**：

```bash
export JWT_SECRET=$(openssl rand -base64 32)
```

详细部署文档见 [docs/deployment-guide.md](docs/deployment-guide.md)。

## CI/CD

| 工作流 | 状态 |
|---|---|
| [![Backend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml) | Backend compile → test → package |
| [![Frontend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml) | Frontend typecheck → build |
| [![Docker Build](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml) | Docker 镜像构建 → GHCR |

- PR 触发：仅 build/test，不推送镜像
- main 分支 / v* 标签推送：build + test + push images to GHCR

### 镜像

```
ghcr.io/yc20041001/ai-coding-platform-backend:latest
ghcr.io/yc20041001/ai-coding-platform-frontend:latest
```

## API 文档

### 审计日志

| Method | Endpoint | 权限 |
|--------|----------|------|
| GET | /api/audit/logs | ADMIN |
| GET | /api/projects/{id}/audit/logs | ADMIN |

### 可观测性

| Method | Endpoint | 权限 |
|--------|----------|------|
| GET | /api/observability/overview | ADMIN |
| GET | /api/projects/{id}/observability/overview | ADMIN |
| GET | /api/observability/model-usage/summary | ADMIN |
| GET | /api/projects/{id}/observability/model-usage/summary | ADMIN |
| GET | /api/projects/{id}/observability/model-usage/daily | ADMIN |

## 数据库

Flyway 自动管理数据库迁移。迁移文件位于：

```
backend/src/main/resources/db/migration/
```

当前迁移版本：
- V1: 认证授权表
- V2: 开发环境管理员种子数据
- V3: 项目表
- V4: 仓库表
- V5: Agent 和 Task 表
- V6: Chat 会话/消息/引用表
- V7: 编排器和模型网关表
- V8: RAG 知识库表
- V9: 审计日志表
- V10: 模型请求日志增强（fallback/错误码/成本估算）

## 模型网关

模型网关提供统一的 LLM 调用入口，支持多供应商、流式输出、安全检测、回退和成本估算。

### 支持的供应商

| 供应商 | 支持非流式 | 支持流式 | 需要 API Key |
|--------|-----------|---------|-------------|
| MOCK | Yes | Yes | No |
| OpenAI Compatible | Yes | Yes | Yes |
| Claude (Anthropic) | Yes | Yes | Yes |
| DeepSeek | Yes | Yes | Yes |
| Qwen (通义千问) | Yes | Yes | Yes |
| Gemini (Google) | Yes | Yes | Yes |

### 环境变量

```bash
# 模型网关通用配置
MODEL_GATEWAY_PROVIDER=MOCK          # 默认供应商 (MOCK / CLAUDE / OPENAI_COMPATIBLE)
MODEL_GATEWAY_TIMEOUT_MS=60000       # 请求超时 (ms)
MODEL_GATEWAY_RETRY_TIMES=1          # 重试次数 (仅网络/超时/限流)

# OpenAI Compatible (支持 OpenAI / DeepSeek / Qwen / Gemini)
OPENAI_ENABLED=false
OPENAI_API_KEY=
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4.1-mini

# Claude (Anthropic)
CLAUDE_ENABLED=false
CLAUDE_API_KEY=
CLAUDE_BASE_URL=https://api.anthropic.com
CLAUDE_MODEL=claude-3-5-sonnet-latest

# DeepSeek
DEEPSEEK_ENABLED=false
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
DEEPSEEK_MODEL=deepseek-chat

# Qwen (通义千问)
QWEN_ENABLED=false
QWEN_API_KEY=
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_MODEL=qwen-plus

# Gemini (Google)
GEMINI_ENABLED=false
GEMINI_API_KEY=
GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
GEMINI_MODEL=gemini-2.5-flash
```

### API Key 安全

- API Key 明文仅通过环境变量或后台数据库存储
- 所有响应和日志中 API Key 自动脱敏（`sk-****abcd`）
- 前端配置页面仅显示脱敏后的 Key
- Prompt Safety 高危拦截后不触发 Fallback

### 模型配置管理 API (ADMIN)

| Method | Endpoint | 说明 |
|--------|----------|------|
| GET | /api/model-gateway/providers | 获取所有供应商能力 |
| GET | /api/model-gateway/configs | 获取所有模型配置 |
| POST | /api/model-gateway/configs | 创建/更新模型配置 |
| PUT | /api/model-gateway/configs/{id} | 更新模型配置 |
| DELETE | /api/model-gateway/configs/{id} | 删除模型配置 |
| POST | /api/model-gateway/test-connection | 测试模型连接 |
| GET | /api/observability/model-usage/cost-summary | 全局用量成本 |
| GET | /api/projects/{id}/observability/model-usage/cost-summary | 项目用量成本 |

### Prompt Safety

请求经模型网关前会进行安全检查：

- **高危拦截** (32 种模式)：永久拦截，不触发 Fallback，返回 `SAFETY_REJECTED`
- **警告模式** (10 种模式)：允许通过但记录日志

### 回退策略

1. Provider 不可用 / 未启用 → Fallback 到 MOCK
2. 网络超时 / 限流 → 自动重试后 Fallback 到 MOCK
3. Prompt Safety 拦截 → 不 Fallback，直接拒绝
4. 请求级 `fallbackEnabled=false` → 跳过 Fallback，返回错误

## 常见问题

**Q: 启动报 "Access denied for user"？**
确认 MySQL 已通过 Docker Compose 启动，且 `DB_PASSWORD` 与 docker-compose.yml 中一致。

**Q: Flyway 迁移失败？**
检查数据库 `ai_coding_platform` 是否存在，或手动创建后重启。

**Q: JWT secret too short？**
确保 `JWT_SECRET` 至少 256 位（32 字符）。

**Q: 模型调用失败？**
默认使用 Mock Provider，无需真实 API Key。如需真实模型，配置对应的 `*_ENABLED=true` 和 `*_API_KEY`。

**Q: SSE 流式输出无响应？**
检查 `MODEL_GATEWAY_PROVIDER` 配置。Mock 模式下会有 150ms 字符间隔输出。
