# Milestone 19: Release Demo & QA Acceptance

## 1. 背景

项目已经完成后端核心能力、前端控制台、自动化测试、Docker Compose、GitHub Actions 与基础容器化发布能力。

当前状态：

- 后端模块：Auth、Project、Member、Repository、Task、Agent、Chat、RAG、Model Gateway、GitHub PR Review、Audit、Observability。
- 前端模块：登录、Dashboard、Projects、Members、Repository、Tasks、Chat SSE、Knowledge、Agents、Model Gateway、GitHub、Observability。
- 测试能力：后端集成测试、前端 typecheck/build/E2E、统一 smoke test、Docker smoke test。
- 交付能力：Dockerfile、Compose、GitHub Actions、GHCR 镜像构建。

Milestone 19 的目标是：

> 将当前项目从“功能已经实现”收口到“可以稳定演示、可以稳定验收、可以安全交给别人运行”的状态。

本阶段不继续新增大模块，重点做发布前验收、演示脚本、质量门禁和问题清单。

## 2. 严格约束

执行 Milestone 19 时必须遵守：

1. 不新增大业务模块。
2. 不重写前端 UI。
3. 不改已验证通过的核心业务逻辑，除非发现明确 bug。
4. 不接真实模型，不提交真实 API Key。
5. 不做 Git 写操作、真实仓库 push、真实 PR 修改等外部副作用。
6. 不引入 Kubernetes、Terraform、云厂商部署配置。
7. 不把 `backend/target`、`frontend/dist`、`node_modules`、`.env` 提交到 Git。
8. 不为了让测试通过而跳过关键测试。
9. 如果需要修复 bug，必须说明原因、影响范围和验证结果。
10. 所有验收命令必须可复制执行。

## 3. 本阶段交付物

建议交付以下内容：

```text
docs/milestone-19-release-demo-qa-acceptance.md
docs/release-demo-runbook.md
docs/release-qa-report.md
scripts/release-demo-check.sh
```

如果不新增脚本，也必须在 `docs/release-qa-report.md` 中完整记录手动执行结果。

## 4. 验收范围

### 4.1 后端验收

必须覆盖：

- 编译
- 集成测试
- 打包
- Flyway 迁移
- Actuator health/info
- 统一响应体
- JWT 登录与鉴权
- Project/Task/Chat/RAG/Model Gateway/GitHub PR Review 主链路

推荐命令：

```bash
cd backend
mvn clean compile
mvn test
mvn package -DskipTests
```

预期：

```text
BUILD SUCCESS
Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
```

### 4.2 前端验收

必须覆盖：

- TypeScript 类型检查
- 生产构建
- Playwright E2E
- 登录态
- 路由守卫
- SSE 页面退出中断
- 页面空状态、错误状态、loading 状态

推荐命令：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e
```

预期：

```text
typecheck pass
build pass
Playwright tests pass
```

### 4.3 Docker / Compose 验收

必须覆盖：

- 后端镜像构建
- 前端镜像构建
- Compose 全栈启动
- backend healthcheck healthy
- frontend healthcheck healthy
- MySQL/Redis/RabbitMQ healthy
- 前端首页可访问
- 后端 health 可访问
- Docker smoke test 通过

推荐命令：

```bash
docker build -t ai-coding-platform-backend:local ./backend
docker build -t ai-coding-platform-frontend:local ./frontend
docker compose -f deploy/docker-compose.app.yml up -d --build
docker compose -f deploy/docker-compose.app.yml ps
curl http://localhost:8080/actuator/health
curl -I http://localhost:5173
bash scripts/docker-smoke-test.sh
```

预期：

```text
backend: healthy
frontend: healthy
mysql: healthy
redis: healthy
rabbitmq: healthy
docker-smoke-test: PASS
```

### 4.4 GitHub Actions 验收

必须检查最新 `main` 分支 push 后的 Actions：

- Backend CI
- Frontend CI
- Docker Build

推荐命令：

```bash
gh run list --repo yc20041001/ai-coding-platform --limit 10
```

预期：

```text
Backend CI: success
Frontend CI: success
Docker Build: success
```

如果失败：

```bash
gh run view <run-id> --repo yc20041001/ai-coding-platform --log-failed
```

必须定位根因并修复，不允许只重跑。

## 5. 演示数据验收

### 5.1 数据初始化

使用已有脚本：

```bash
bash scripts/dev-seed-demo-data.sh
```

验收点：

- Demo Project 存在。
- Demo Knowledge Base 存在。
- Demo Document 已上传并完成切片。
- Demo Chat Session 存在。
- Demo Task 存在。
- 脚本重复执行幂等，不产生不可控重复数据。

### 5.2 数据重置

仅在本地演示库使用：

```bash
bash scripts/dev-reset-db.sh --yes
```

注意：

- 不允许对非本地库执行。
- 不允许在生产或真实用户数据环境执行。
- 执行后必须重新跑 Flyway 和 seed。

## 6. 手动浏览器验收清单

访问：

```text
http://localhost:5173
```

默认账号：

```text
admin@example.com / Admin@123456
```

逐页检查：

| # | 页面/功能 | 验收点 | 预期 |
|---|---|---|---|
| 1 | Login | 正确账号登录 | 成功进入 Dashboard |
| 2 | Login | 错误密码 | 显示错误，不进入系统 |
| 3 | Dashboard | 系统概览 | 指标卡正常加载 |
| 4 | Projects | 项目列表 | 能看到项目，能创建项目 |
| 5 | Project Detail | Tabs | Overview/Tasks/Chat/Knowledge/Repository/Members 可切换 |
| 6 | Members | 成员列表 | admin 为 OWNER |
| 7 | Repository | 仓库空状态/绑定状态 | 无仓库时显示空状态，有仓库时显示分支/操作 |
| 8 | Tasks | 创建任务 | 返回 PENDING |
| 9 | Tasks | 执行任务 | 状态变为 COMPLETED，产生日志/产物/执行记录 |
| 10 | Task Detail | Logs/Artifacts/Executions/Model Logs | 均可打开查看 |
| 11 | Chat | 创建会话 | 会话创建成功 |
| 12 | Chat | 发送消息 | SSE token 流正常，done 后消息落库 |
| 13 | Chat | References | RAG 命中时引用展示正常 |
| 14 | Knowledge | 创建 KB | 成功创建 ACTIVE 知识库 |
| 15 | Knowledge | 上传文档 | 文档 COMPLETED，chunk 可预览 |
| 16 | Knowledge | RAG 搜索 | 命中结果显示 score/snippet/filePath |
| 17 | Agents | Agent 列表 | 6 个内置 Agent 可见 |
| 18 | Model Gateway | Provider/配置页 | MOCK 配置可见，连接测试交互正常 |
| 19 | GitHub | OAuth 未配置状态 | 显示清晰提示，不影响系统 |
| 20 | GitHub PR Review | 页面打开 | 不因未配置 OAuth 崩溃 |
| 21 | Observability | ADMIN 可见 | 指标、模型用量、审计日志加载 |
| 22 | Logout | 退出登录 | token 清理，回到 Login |

## 7. 负向与降级验收

必须覆盖：

| 场景 | 操作 | 预期 |
|---|---|---|
| 未登录访问 API | `curl http://localhost:8080/api/projects` | `UNAUTHORIZED` |
| access token 调 refresh | 使用 accessToken 请求 `/api/auth/refresh` | `UNAUTHORIZED` |
| 重复执行已完成任务 | 再次调用 execute | `CONFLICT` |
| RAG 无结果 | 搜索不存在关键词 | 返回空结果，不报错 |
| RAG disabled/useRag=false | Chat/Task 关闭 RAG | 主流程继续 |
| Model Provider 无真实 Key | Provider 不可用 | fallback 或清晰错误 |
| GitHub OAuth 未配置 | 打开 GitHub 页面 | 系统正常，显示未配置提示 |
| 前端刷新深层路由 | 刷新 `/projects/:id/tasks/:taskId` | 不 404 |

## 8. 发布质量门禁

Milestone 19 通过条件：

- `mvn test` 全绿。
- `npm run typecheck` 全绿。
- `npm run build` 成功。
- `npm run test:e2e` 全绿。
- `scripts/docker-smoke-test.sh` 成功。
- Docker Compose 所有服务 healthy。
- GitHub Actions 最新提交三条 workflow 全绿。
- 浏览器手动验收核心路径通过。
- 未发现真实密钥进入 Git。
- `.env`、`target/`、`dist/`、`node_modules/` 未进入 Git。
- 已知限制已记录。

任何一个必选项失败，都不能标记 Milestone 19 完成。

## 9. 建议新增 release check 脚本

可以新增：

```text
scripts/release-demo-check.sh
```

建议内容：

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "== Backend checks =="
(cd backend && mvn clean compile && mvn test && mvn package -DskipTests)

echo "== Frontend checks =="
(cd frontend && npm run typecheck && npm run build)

echo "== Docker smoke test =="
bash scripts/docker-smoke-test.sh

echo "== Git status =="
git status --short

echo "Release demo check completed."
```

注意：是否执行 Playwright E2E 可通过参数控制，因为它依赖前后端服务状态。

## 10. 输出报告要求

完成后必须新增或更新：

```text
docs/release-qa-report.md
```

报告格式：

```markdown
# Release QA Report

## 1. 环境信息
- 日期:
- Git commit:
- Java:
- Node:
- Docker:
- OS:

## 2. 自动化检查结果
| 检查项 | 命令 | 结果 |
|---|---|---|

## 3. Docker/Compose 验收
| 服务 | 状态 | 端口 |
|---|---|---|

## 4. GitHub Actions 结果
| Workflow | Run ID | Status |
|---|---|---|

## 5. 浏览器手动验收
| 场景 | 结果 | 备注 |
|---|---|---|

## 6. 发现并修复的问题
| 问题 | 原因 | 修复 | 验证 |
|---|---|---|---|

## 7. 已知限制

## 8. 结论
是否具备本地演示/交付验收条件：
```

## 11. 已知限制模板

验收报告中至少检查并记录：

- 真实模型 Provider 是否配置。
- GitHub OAuth 是否配置。
- Docker Hub/GHCR 网络是否稳定。
- 前端大 chunk warning 是否存在。
- Redis/RabbitMQ 是否只是预留依赖。
- Member 邀请是否仍为 Mock。
- Repository clone/pull 是否依赖外部网络。
- 生产 CORS/JWT/密钥是否还需替换。

## 12. Claude 执行提示词

可以直接把下面这段发给 Claude：

```text
请根据项目中的文档执行 Milestone 19。

文档路径：
docs/milestone-19-release-demo-qa-acceptance.md

执行要求：
1. 先完整阅读该文档，再检查当前项目代码、脚本、Docker Compose、GitHub Actions 和 README。
2. 本阶段是发布演示与 QA 验收收口，不新增大业务模块。
3. 不重写前端 UI，不改已验证通过的核心业务逻辑。
4. 不接真实模型，不提交真实 API Key，不写真实生产密钥。
5. 不执行真实 Git 写操作、真实 GitHub PR 修改、真实仓库写入操作。
6. 可以修复明确的环境、脚本、CI、Docker healthcheck、测试配置等问题，但必须说明原因和影响范围。
7. 不要把 backend/target、frontend/dist、node_modules、.env 纳入 Git。
8. 优先复用已有脚本：
   - scripts/run-backend-checks.sh
   - scripts/run-frontend-checks.sh
   - scripts/run-all-checks.sh
   - scripts/backend-unified-smoke-test.sh
   - scripts/docker-smoke-test.sh
   - scripts/dev-seed-demo-data.sh
9. 如果新增脚本，必须使用安全默认值，并保证可重复执行。
10. 如果 Docker 或 GitHub Actions 因环境限制无法执行，必须记录原因、已替代验证项和待补验步骤。

必须执行或验证：
1. cd backend && mvn clean compile
2. cd backend && mvn test
3. cd backend && mvn package -DskipTests
4. cd frontend && npm run typecheck
5. cd frontend && npm run build
6. 如环境允许，cd frontend && npm run test:e2e
7. docker build -t ai-coding-platform-backend:local ./backend
8. docker build -t ai-coding-platform-frontend:local ./frontend
9. docker compose -f deploy/docker-compose.app.yml up -d --build
10. docker compose -f deploy/docker-compose.app.yml ps
11. curl http://localhost:8080/actuator/health
12. curl -I http://localhost:5173
13. bash scripts/docker-smoke-test.sh
14. gh run list --repo yc20041001/ai-coding-platform --limit 10

需要检查浏览器页面：
1. Login
2. Dashboard
3. Projects
4. Project Detail Tabs
5. Members
6. Repository
7. Tasks
8. Task Detail
9. Chat SSE
10. Knowledge + RAG Search
11. Agents
12. Model Gateway
13. GitHub Integration / PR Review
14. Observability
15. Logout

完成后必须新增或更新：
docs/release-qa-report.md

完成后按以下格式输出：
1. 新增/修改文件清单
2. 自动化检查结果
3. Docker / Compose 验收结果
4. GitHub Actions 验收结果
5. 浏览器手动验收结果
6. 发现并修复的问题
7. 未修复问题与已知限制
8. 是否具备本地演示/发布验收条件
9. 是否可以进入 Milestone 20：UI 视觉升级 Phase 2 或真实部署

现在开始执行，不要只给计划。
```

