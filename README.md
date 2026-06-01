# AI Coding Platform

**面向项目、智能体、知识库与代码评审的 AI 原生协作工作台。**

这是一个统一的协作控制台，用来把项目上下文、知识库 RAG、智能体任务执行以及 GitHub PR Review 串起来，并提供完整的可观测性与审计追踪能力。

[![Backend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml)
[![Docker Build](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml)

**当前状态：** Internal Alpha v1.0，功能可用且已完成测试，适合演示与试用。  
[公开概览页](http://localhost:5173/public) · [快速体验](#演示快速开始) · [路线图](docs/roadmap.md) · [变更日志](CHANGELOG.md)

> **默认使用 Mock Provider。** 你无需配置 API Key 也可以先体验整套系统。  
> 如果要接入真实模型（OpenAI、Claude、DeepSeek 等），需要显式配置。GitHub OAuth 是可选项。数据处理说明见下方[安全](#安全)章节。

---

## 核心能力

| 模块 | 说明 | 状态 |
|------|------|------|
| **项目工作台** | 多标签控制台：概览、任务、聊天、知识库、仓库、成员 | 就绪 |
| **知识库与 RAG** | 文档上传、自动切块、向量检索、相关度评分 | 就绪 |
| **SSE 流式聊天** | 实时流式回复，支持 RAG 引用高亮与来源追踪 | 就绪 |
| **智能体任务执行** | 支持 FEATURE / BUGFIX / REVIEW / REFACTOR 任务，完整状态机与产物追踪 | 就绪 |
| **模型网关** | 统一接入 OpenAI、Claude、DeepSeek、Qwen、Gemini 与 Mock；支持 fallback、成本估算与安全过滤 | 就绪 |
| **GitHub PR Review** | 只读 OAuth 集成，支持仓库浏览与 AI 辅助 PR 评审 | 就绪 |
| **可观测性与审计** | 系统指标、项目级模型用量与成本、审计日志、模型请求追踪 | 就绪 |

---

## 演示快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/yc20041001/ai-coding-platform.git
cd ai-coding-platform

# 2. 启动基础设施（MySQL、Redis、RabbitMQ）
docker compose -f deploy/docker-compose.yml up -d

# 3. 启动后端
cd backend && source ../.env && mvn spring-boot:run

# 4. 启动前端（新开终端）
cd frontend && npm install && npm run dev -- --host 0.0.0.0

# 5. 初始化演示数据（新开终端）
bash scripts/demo-seed-data.sh
```

打开 **http://localhost:5173**，使用以下账号登录：

- 邮箱：`admin@example.com`
- 密码：`Admin@123456`

之后可以参考 [Demo Walkthrough](docs/demo-walkthrough.md) 按引导体验完整流程。  
如果你想看 Docker 试用模式或生产演示模式，请查看 [Trial Entry Guide](docs/trial-entry-guide.md)。

---

## 系统架构

```text
前端控制台（Vue 3 SPA，暗色科技风 UI，SSE 流式）
  → Spring Boot API（REST 控制器，JWT 鉴权，RBAC）
  → 核心模块（Project、Task、Chat、RAG、Agent、Repository）
  → 模型网关（多 Provider、Fallback、成本追踪、安全过滤）
  → 外部集成（GitHub OAuth、PR Review）
  → 基础设施（MySQL 8、Redis 7、RabbitMQ、Docker）

可观测性与审计贯穿全部层级。
```

---

## 安全

- **默认 Mock Provider**：未配置真实模型前，数据不会离开本机
- **API Key 全量脱敏**：所有响应与日志中都会进行遮罩（如 `sk-****abcd`）
- **不提交密钥**：`.env`、`.env.production`、证书与 key 文件都已加入 `.gitignore`
- **Prompt Safety**：高风险模式会在请求到达模型前被拦截
- **JWT 鉴权**：所有 API 端点均受保护，没有后门入口
- **GitHub OAuth 为只读模式**：不会自动评论、提交或 push
- **真实模型必须显式配置**：需要设置 `*_ENABLED=true` 与 `*_API_KEY`

生产部署的安全加固请查看：[Security Hardening Checklist](docs/production-security-hardening-checklist.md)

## 登录与权限安全

- **登录验证码**：4 位验证码，可配置过期时间和尝试次数限制（Redis 优先，内存回退）
- **登录防暴力破解**：基于邮箱 / IP 的尝试次数限制（默认 5 次失败锁定 10 分钟）
- **JWT Token**：支持 Access Token + Refresh Token 的完整流程
- **RBAC 权限模型**：基于角色的项目与模块权限控制

---

## 反馈与迭代

- [Bug Report](https://github.com/yc20041001/ai-coding-platform/issues/new?template=bug_report.yml)
- [Feature Request](https://github.com/yc20041001/ai-coding-platform/issues/new?template=feature_request.yml)
- [Trial Feedback](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)
- [反馈分类体系](docs/product-feedback-taxonomy.md)
- [反馈分流指南](docs/user-trial-triage-guide.md)
- [路线图](docs/roadmap.md)
- [变更日志](CHANGELOG.md)
- [Alpha / Beta 试用计划](docs/alpha-beta-trial-plan.md)

---

## 技术栈

| 组件 | 版本 / 技术 |
|------|-------------|
| Java | 17 |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.7 |
| Flyway | 10.20.1 |
| MySQL | 8.0 |
| Redis | 7 |
| RabbitMQ | 3（management） |
| Maven | 3.x |
| JWT | jjwt 0.12.x |
| Spring Security | 6.x |
| Frontend | Vue 3 + TypeScript + Vite + Element Plus + Pinia |

---

## 项目结构

```text
ai-coding-platform/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/aicoding/platform/
│       ├── agent/              # 智能体模块
│       ├── audit/              # 审计日志模块
│       ├── auth/               # 鉴权与认证
│       ├── chat/               # 聊天会话 / 消息 / SSE
│       ├── common/             # 通用基础（异常 / 分页 / 响应）
│       ├── member/             # 项目成员 / 权限
│       ├── modelgateway/       # 模型网关（多 Provider）
│       ├── observability/      # 可观测性（用量 / 概览）
│       ├── orchestrator/       # 智能体编排
│       ├── project/            # 项目模块
│       ├── rag/                # RAG 知识库
│       ├── repository/         # 代码仓库（GitHub）
│       ├── security/           # 安全（JWT / Filter）
│       └── task/               # 任务模块
├── frontend/                   # Vue 3 企业控制台
│   └── src/modules/
│       ├── admin/              # 可观测性 / 审计日志
│       ├── agent/              # 智能体管理
│       ├── auth/               # 登录 / 鉴权状态
│       ├── chat/               # 聊天会话（SSE）
│       ├── dashboard/          # 仪表盘
│       ├── knowledge/          # 知识库
│       ├── member/             # 成员管理
│       ├── project/            # 项目管理
│       ├── public/             # 公开入口页（/public）
│       ├── repository/         # 仓库管理
│       └── task/               # 任务管理 / 执行详情
├── deploy/                     # 部署配置
│   └── docker-compose.yml      # 本地 Docker Compose
├── docs/                       # 文档
├── scripts/                    # 工具脚本
├── .env.example                # 环境变量模板
└── README.md                   # 当前文档
```

---

## 本地依赖

启动应用前，请先启动基础设施：

```bash
# 启动全部依赖
docker compose -f deploy/docker-compose.yml up -d

# 查看状态
docker compose -f deploy/docker-compose.yml ps

# 停止服务
docker compose -f deploy/docker-compose.yml down
```

- MySQL 8.0（端口 3307）
- Redis 7（端口 6379）
- RabbitMQ 3（端口 5672，管理台 15672）

---

## 环境变量

```bash
cp .env.example .env
source .env
```

关键环境变量如下：

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_URL | 数据库连接串 | jdbc:mysql://127.0.0.1:3307/ai_coding_platform |
| DB_USERNAME | 数据库用户名 | root |
| DB_PASSWORD | 数据库密码 | platform123 |
| JWT_SECRET | JWT 签名密钥 | （至少 256 位） |
| MODEL_GATEWAY_PROVIDER | 默认模型 Provider | MOCK |
| MODEL_GATEWAY_TIMEOUT_MS | 请求超时（毫秒） | 60000 |
| MODEL_GATEWAY_RETRY_TIMES | 重试次数 | 1 |
| OPENAI_ENABLED | 是否启用 OpenAI | false |
| OPENAI_API_KEY | OpenAI API Key | 需自行配置 |
| CLAUDE_ENABLED | 是否启用 Claude | false |
| CLAUDE_API_KEY | Claude API Key | 需自行配置 |
| DEEPSEEK_ENABLED | 是否启用 DeepSeek | false |
| DEEPSEEK_API_KEY | DeepSeek API Key | 需自行配置 |
| QWEN_ENABLED | 是否启用 Qwen | false |
| QWEN_API_KEY | Qwen API Key | 需自行配置 |
| GEMINI_ENABLED | 是否启用 Gemini | false |
| GEMINI_API_KEY | Gemini API Key | 需自行配置 |

---

## 后端启动

```bash
cd backend
mvn clean compile
mvn test
source ../.env
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

登录获取 token：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'
```

---

## 前端启动

```bash
cd frontend
npm install
cp .env.example .env
npm run dev -- --host 0.0.0.0
```

打开：http://localhost:5173

**公开入口：** http://localhost:5173/public ，无需登录，可直接查看产品概览。

默认登录账号：

```text
Email: admin@example.com
Password: Admin@123456
```

更多前端说明见：[frontend/README.md](frontend/README.md)

---

## 测试

| 层级 | 技术 | 命令 |
|------|------|------|
| 后端集成测试 | JUnit 5 + Spring Boot Test | `cd backend && mvn test` |
| 前端 E2E | Playwright | `cd frontend && npm run test:e2e` |
| Smoke Test | Shell（curl） | `bash scripts/demo-smoke-test.sh` |

### 质量门

| 质量门 | 类型 | 命令 |
|--------|------|------|
| 后端测试 | **阻塞** | `cd backend && mvn test` |
| 前端 typecheck | **阻塞** | `cd frontend && npm run typecheck` |
| 前端构建 | **阻塞** | `cd frontend && npm run build` |
| E2E 测试（×2） | **阻塞** | `cd frontend && npm run test:e2e -- --workers=1` |
| Bundle 检查 | 警告 | `bash scripts/frontend-bundle-check.sh` |
| Secret 扫描 | **阻塞** | 已集成在 `scripts/release-checklist.sh` |

更多说明见：
- [Testing Strategy](docs/testing-strategy.md)
- [Backend Testing Guide](docs/backend-testing-guide.md)
- [Backend Test Matrix](docs/backend-test-matrix.md)

### 一键检查脚本

```bash
bash scripts/run-backend-checks.sh   # 后端 compile + test + package
bash scripts/run-frontend-checks.sh  # 前端 typecheck + build + E2E
bash scripts/run-all-checks.sh       # 全量检查
```

### 演示脚本

```bash
bash scripts/demo-seed-data.sh        # 初始化演示数据（幂等）
bash scripts/demo-smoke-test.sh       # 执行演示 smoke test
bash scripts/demo-reset-data.sh --yes # 清理演示数据
```

更多演示资料见：
- [Demo Data Guide](docs/demo-data-guide.md)
- [Demo Walkthrough](docs/demo-walkthrough.md)
- [Acceptance Checklist](docs/demo-acceptance-checklist.md)
- [Feedback Template](docs/user-feedback-template.md)

---

## Docker 部署

### 全栈启动

```bash
docker compose -f deploy/docker-compose.app.yml up -d --build
docker compose -f deploy/docker-compose.app.yml logs -f backend
docker compose -f deploy/docker-compose.app.yml down
```

### 生产模式

```bash
cp .env.production.example .env.production
# 编辑 .env.production，把所有 CHANGE_ME 替换成真实值
bash scripts/prod-deploy.sh up --build
bash scripts/prod-smoke-test.sh http://localhost
```

访问入口：

| 服务 | 地址 |
|------|------|
| 前端控制台 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| 健康检查 | http://localhost:8080/actuator/health |
| RabbitMQ 管理台 | http://localhost:15672 |

**生产运维资料：** [Health Check](scripts/prod-health-check.sh) · [Alerting](docs/production-alerting-rules.md) · [Observability](docs/production-observability-runbook.md) · [Security Hardening](docs/production-security-hardening-checklist.md) · [Incident Response](docs/incident-response-runbook.md)

---

## 文档与移交

**最终交付包（v1.0 Alpha）：**

| 文档 | 用途 |
|------|------|
| [Final Delivery Report](docs/final-delivery-report.md) | 完成状态、模块清单、质量门、已知限制 |
| [Project Handoff Guide](docs/project-handoff-guide.md) | Day 1 启动、测试、排障、发布说明 |
| [Documentation Index](docs/documentation-index.md) | 全部文档目录（约 70 份） |
| [API / Page / Script Index](docs/api-page-script-index.md) | 所有接口、页面路由和工具脚本索引 |
| [Environment Variable Index](docs/environment-variable-index.md) | 全部环境变量与安全等级说明 |
| [Final Release Checklist](docs/final-release-checklist.md) | 10 大类发布前检查清单 |

**快速索引：** [Known Limitations](docs/final-delivery-report.md#6-known-limitations) · [Quality Gates](docs/final-delivery-report.md#4-quality-gate-summary) · [Deployment Modes](docs/final-delivery-report.md#5-deployment-modes) · [Roadmap](docs/roadmap.md) · [Changelog](CHANGELOG.md)

---

## CI/CD

| Workflow | 说明 |
|----------|------|
| [![Backend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/backend-ci.yml) | 后端 compile → test → package |
| [![Frontend CI](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/frontend-ci.yml) | 前端 typecheck → build |
| [![Docker Build](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml/badge.svg)](https://github.com/yc20041001/ai-coding-platform/actions/workflows/docker-build.yml) | Docker 镜像构建 → 推送 GHCR |

镜像地址：

```text
ghcr.io/yc20041001/ai-coding-platform-backend:latest
ghcr.io/yc20041001/ai-coding-platform-frontend:latest
```

---

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

---

## 数据库

数据库结构由 Flyway 管理：

```text
backend/src/main/resources/db/migration/
```

主要迁移版本：

- V1：认证相关表
- V2：管理员初始化数据
- V3：项目相关表
- V4：仓库相关表
- V5：智能体与任务表
- V6：聊天会话 / 消息 / 引用表
- V7：编排器与模型网关表
- V8：RAG 知识库表
- V9：审计日志表
- V10：模型请求日志增强（fallback / error code / cost estimation）

---

## 模型网关

统一的多 Provider LLM 接入层：

| Provider | 非流式 | 流式 | 是否需要 API Key |
|----------|--------|------|------------------|
| MOCK | 是 | 是 | 否 |
| OpenAI Compatible | 是 | 是 | 是 |
| Claude (Anthropic) | 是 | 是 | 是 |
| DeepSeek | 是 | 是 | 是 |
| Qwen | 是 | 是 | 是 |
| Gemini (Google) | 是 | 是 | 是 |

### API Key 安全

- 所有响应与日志中的 API Key 都会被脱敏（如 `sk-****abcd`）
- 前端配置页仅展示脱敏后的密钥
- Prompt Safety 会在请求到达模型前拦截高风险模式

### Fallback 策略

1. Provider 不可用 / 未启用 → fallback 到 MOCK
2. 网络超时 / 限流 → 自动重试，失败后 fallback 到 MOCK
3. Prompt Safety 拒绝 → 不 fallback，直接拒绝
4. 请求级 `fallbackEnabled=false` → 跳过 fallback，直接返回错误

---

## 常见问题

**Q：启动时报 `Access denied for user` 怎么办？**  
确认 MySQL 已通过 Docker Compose 启动，并且 `DB_PASSWORD` 与 `docker-compose.yml` 中一致。

**Q：Flyway migration 失败怎么办？**  
确认数据库 `ai_coding_platform` 已创建；如果没有，先创建后再重启。

**Q：JWT secret 太短怎么办？**  
请确保 `JWT_SECRET` 至少为 256 位（32 个字符以上）。

**Q：模型调用失败怎么办？**  
默认 Provider 是 Mock，不需要 API Key。若要使用真实模型，请配置 `*_ENABLED=true` 与 `*_API_KEY`。

**Q：SSE 流式回复没有返回怎么办？**  
检查 `MODEL_GATEWAY_PROVIDER` 配置。Mock 模式下默认按约 150ms 字符间隔输出。  
