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
| OPENAI_ENABLED | 启用 OpenAI | false |
| OPENAI_API_KEY | OpenAI API Key | (需自行配置) |

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
