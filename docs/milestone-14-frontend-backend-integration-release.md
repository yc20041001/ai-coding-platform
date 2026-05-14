# Milestone 14: 前后端联调与发布准备实施文档

## 1. 背景与目标

Milestone 1-11 已完成后端核心能力：

- Auth / JWT / RBAC
- Project / Member
- Repository
- Task / Agent
- Chat + SSE
- Agent Orchestrator + Model Gateway
- RAG Knowledge Base
- Audit / Observability

Milestone 12-13 已完成前端 Vue 3 企业级控制台：

- 登录与路由守卫
- Dashboard
- Project / Member / Repository
- Task / Agent Execution / Model Logs
- Chat SSE
- Knowledge Base / RAG Search
- Observability / Audit Logs

当前系统已经具备完整业务链路，但还需要进行系统性联调、环境固化、构建验证和发布准备。

Milestone 14 的目标是：

> 完成前后端全链路联调与发布前质量收口，确保本地开发环境可一键启动、核心业务流程可稳定验证、前后端配置清晰、构建产物可发布。

## 2. 实施边界

### 2.1 本阶段要做

- 固化前后端本地启动流程
- 统一前后端环境变量配置
- 校验 CORS / JWT / API Base URL
- 完成前后端主链路联调
- 补齐 smoke test 脚本或文档
- 验证前端 typecheck / build
- 验证后端 compile / test
- 验证 Flyway 迁移
- 验证 Docker Compose 本地依赖
- 输出发布准备清单
- 输出已知问题与后续计划

### 2.2 本阶段不做

- 不新增大业务模块
- 不接入新的模型 Provider
- 不重构后端模块边界
- 不重写前端 UI
- 不做生产 Kubernetes 部署
- 不做 CI/CD 全量流水线
- 不做 HTTPS / 域名 / 证书配置
- 不做真实邮件、真实 GitHub OAuth、真实企业 SSO

## 3. 交付物

本阶段完成后，应至少交付：

```text
docs/milestone-14-frontend-backend-integration-release.md
docs/release-readiness-checklist.md              # 可选
scripts/backend-unified-smoke-test.sh             # 已有则维护
scripts/frontend-smoke-test.md 或 docs/frontend-smoke-test-plan.md
frontend/.env.example                             # 确认存在并可用
.env.example                                      # 确认后端/基础设施配置完整
README.md                                         # 更新启动与验证说明
```

如需要自动化程度更高，可以新增：

```text
scripts/dev-start-backend.sh
scripts/dev-start-frontend.sh
scripts/dev-smoke-test.sh
scripts/release-check.sh
```

## 4. 环境要求

### 4.1 基础环境

```text
Java 17
Maven 3.9+
Node.js 20+
npm 10+
MySQL 8.x 或兼容版本
Redis 7.x（当前阶段可选）
RabbitMQ 3.x（当前阶段可选）
Docker Desktop（推荐）
```

### 4.2 默认端口

```text
Backend:  http://localhost:8080
Frontend: http://localhost:5173
MySQL:    127.0.0.1:3306 或 127.0.0.1:3307
Redis:    127.0.0.1:6379
RabbitMQ: 127.0.0.1:5672 / 15672
```

### 4.3 后端环境变量

建议本地开发使用：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USERNAME="aicoding"
export DB_PASSWORD="aicoding123"
export JWT_SECRET="verification-test-secret-min-32bytes"
export MODEL_GATEWAY_PROVIDER="MOCK"
```

如使用 Docker Compose 的 MySQL：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3307/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="platform123"
export JWT_SECRET="verification-test-secret-min-32bytes"
export MODEL_GATEWAY_PROVIDER="MOCK"
```

### 4.4 前端环境变量

`frontend/.env.example` 应包含：

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=AI Coding Platform
```

本地运行时可创建：

```bash
cd frontend
cp .env.example .env.local
```

## 5. 启动流程

### 5.1 启动基础设施

优先使用 Docker Compose：

```bash
docker compose -f deploy/docker-compose.yml up -d
```

检查：

```bash
docker compose -f deploy/docker-compose.yml ps
```

如果本机已有 MySQL 占用 `3307`，可以：

- 临时停止占用端口的 MySQL
- 或改用本机 `3306`
- 或修改 `deploy/docker-compose.yml` 端口映射

### 5.2 启动后端

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

预期：

```json
{"status":"UP"}
```

### 5.3 启动前端

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

浏览器访问：

```text
http://localhost:5173
```

默认登录账号：

```text
邮箱：admin@example.com
密码：Admin@123456
```

## 6. 联调检查项

### 6.1 CORS 检查

```bash
curl -i -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type"
```

必须包含：

```text
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: content-type
```

### 6.2 登录接口检查

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}'
```

预期：

- `code=OK`
- 返回 `accessToken`
- 返回 `refreshToken`
- 返回 `user.roles=["ADMIN"]`

### 6.3 当前用户检查

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

预期：

- `code=OK`
- 返回 `username=admin`
- 返回 `roles`
- 返回 `permissions`

## 7. 核心业务联调路径

### 7.1 Auth

验收项：

- 登录成功后跳转 Dashboard
- Token 写入本地存储
- 刷新页面后仍保持登录
- 无 token 访问业务页跳转 `/login`
- 401 自动清理登录态

### 7.2 Dashboard / Observability

验收项：

- Dashboard 指标卡可加载
- `/api/observability/overview` 返回正常
- ADMIN 用户可以访问 Observability 菜单
- Audit Logs 可分页展示
- Model Usage Summary 可展示

### 7.3 Project / Member

验收项：

- 创建项目成功
- 项目列表显示新项目
- 项目详情可进入
- Members Tab 显示 OWNER 成员
- 邀请成员可创建 pending invitation
- 不能移除自己

### 7.4 Repository

验收项：

- Repository Tab 可打开
- 绑定仓库成功
- Clone / Pull 按钮可调用后端
- Branches 可展示
- Diff 可展示

注意：

- Clone / Pull 依赖网络和 GitHub 仓库可访问性
- 当前阶段不做真实 commit / push / PR

### 7.5 Agent

验收项：

- Agent 列表显示内置 6 个 Agent
- Agent 类型、状态、描述可展示
- 项目级启用 / 禁用 Agent 可用

### 7.6 Task + Orchestrator

验收项：

- 创建 Task 成功
- Task 列表分页正常
- Task 详情可进入
- 执行 Task 成功
- Task 状态从 `PENDING` 流转到 `COMPLETED`
- Logs 展示 `ORCHESTRATOR_START / MODEL_GATEWAY_REQUEST / ORCHESTRATOR_DONE`
- Artifacts 展示 Mock Markdown 产物
- Executions 展示 Agent Execution
- Model Logs 展示 Mock 模型调用日志

### 7.7 Chat + SSE

验收项：

- 创建 Chat Session 成功
- 发送消息成功
- USER 消息落库
- AGENT 消息从 `STREAMING` 到 `COMPLETED`
- SSE token 流式输出正常
- done 事件后消息内容完整保存
- references 可展示

### 7.8 Knowledge Base + RAG

验收项：

- 创建 Knowledge Base 成功
- 上传 Markdown / Text / Code 文档成功
- 文档自动切片
- Chunk 列表可查看
- RAG Search 可返回匹配结果
- Chat 发送消息时 `useRag=true` 可返回 references
- Task 执行时 `useRag=true` 可注入 RAG context

## 8. 自动化验证

### 8.1 后端验证

```bash
cd backend
mvn clean compile
mvn test
```

预期：

```text
BUILD SUCCESS
```

### 8.2 前端验证

```bash
cd frontend
npm install
npm run typecheck
npm run build
```

预期：

```text
typecheck 0 errors
build success
```

### 8.3 后端 Smoke Test

如已有脚本：

```bash
scripts/backend-unified-smoke-test.sh
```

建议覆盖：

- 登录
- 创建项目
- 创建 KB
- 上传文档
- RAG Search
- 创建 Chat Session
- Chat Send + SSE
- 创建 Task
- Execute Task
- 查询 Logs / Artifacts / Executions / Model Logs
- 查询 Audit Logs
- 查询 Observability

### 8.4 前端 Smoke Test

参考：

```text
docs/frontend-smoke-test-plan.md
```

建议至少人工验证：

- Login
- Dashboard
- Project Create / Detail
- Members
- Repository
- Task Create / Execute / Detail
- Chat SSE
- Knowledge Upload / Search
- Agents
- Observability

## 9. 发布准备

### 9.1 后端打包

```bash
cd backend
mvn clean package -DskipTests
```

产物：

```text
backend/target/ai-coding-platform-0.0.1-SNAPSHOT.jar
```

运行：

```bash
java -jar backend/target/ai-coding-platform-0.0.1-SNAPSHOT.jar
```

### 9.2 前端构建

```bash
cd frontend
npm run build
```

产物：

```text
frontend/dist/
```

### 9.3 前端静态预览

```bash
cd frontend
npm run preview -- --host 0.0.0.0
```

访问：

```text
http://localhost:4173
```

### 9.4 发布配置检查

发布前必须确认：

- `JWT_SECRET` 不使用开发默认值
- `DB_PASSWORD` 不写死在代码仓库
- `MODEL_GATEWAY_PROVIDER` 明确配置
- 前端 `VITE_API_BASE_URL` 指向正确后端地址
- CORS 仅允许可信前端域名
- Actuator 暴露范围受控
- 日志不输出 API Key / Token
- `.env` 未提交 Git
- `.DS_Store` / `target/` / `dist/` 已忽略

## 10. 验收标准

### 10.1 必须通过

- 后端 `mvn clean compile` 成功
- 后端 `mvn test` 成功
- 前端 `npm run typecheck` 成功
- 前端 `npm run build` 成功
- Flyway V1-V9 迁移成功
- 登录成功
- Dashboard 正常加载
- 创建项目成功
- 创建任务并执行成功
- Chat SSE 正常输出
- Knowledge Base 上传与检索成功
- Audit Logs 可查询
- Observability 可查看

### 10.2 不允许出现

- 浏览器控制台大量红色报错
- 登录后页面白屏
- 401 后仍停留在业务页
- API Base URL 指向错误
- CORS 阻断前端请求
- SSE 无法完成 done 事件
- Task 执行绕过状态机
- RAG 失败阻断 Chat / Task 主流程
- 后端启动依赖不存在的本地私有路径

## 11. 已知限制

当前阶段允许保留：

- GitHub OAuth 未接真实授权
- Member 邀请不发真实邮件
- Model Gateway 可使用 MOCK
- RAG 使用 Mock Embedding / MySQL LIKE
- Repository 不做真实 commit / push / PR
- 前端未做完整 E2E 自动化
- 前端未做移动端深度适配
- 未做生产级 Nginx / HTTPS / CDN 配置

## 12. 建议执行顺序

给 Claude 执行时建议按以下顺序：

1. 检查当前后端是否可启动
2. 检查当前前端是否可启动
3. 修复环境配置问题
4. 验证 Auth 登录链路
5. 验证核心业务链路
6. 执行后端 compile/test
7. 执行前端 typecheck/build
8. 执行 smoke test
9. 更新 README / .env.example / docs
10. 输出发布准备报告

## 13. 给 Claude 的执行提示词

可以直接发送以下内容给 Claude：

```text
请执行 Milestone 14：前后端联调与发布准备。

请先阅读：
- docs/milestone-14-frontend-backend-integration-release.md
- README.md
- .env.example
- frontend/.env.example
- docs/frontend-smoke-test-plan.md
- docs/unified-backend-regression-test-plan.md

目标：
完成 AI Coding Platform 的前后端全链路联调、环境配置校验、构建验证和发布准备。

要求：
1. 不新增大业务模块。
2. 不重构已验证通过的后端核心逻辑。
3. 不重写前端 UI。
4. 可以修复联调过程中发现的明确 bug。
5. 后端必须通过 mvn clean compile 和 mvn test。
6. 前端必须通过 npm run typecheck 和 npm run build。
7. 验证登录、项目、任务执行、Chat SSE、Knowledge/RAG、Observability 主链路。
8. 检查 CORS、JWT、VITE_API_BASE_URL、DB_URL、Flyway、Actuator。
9. 如发现环境问题，请优先用文档化方式修复，不要写死本机私有路径。
10. 完成后输出：
   - 修改文件清单
   - 修复的问题
   - 联调验证结果
   - 构建结果
   - Smoke Test 结果
   - 已知限制
   - 是否具备本地发布/演示条件

请开始执行。
```

## 14. 完成报告模板

```markdown
# Milestone 14 完成报告

## 1. 修改文件

| 文件 | 说明 |
|---|---|
|  |  |

## 2. 环境验证

| 项目 | 结果 | 说明 |
|---|---|---|
| MySQL |  |  |
| Backend 8080 |  |  |
| Frontend 5173 |  |  |
| CORS |  |  |
| Flyway |  |  |

## 3. 构建验证

| 命令 | 结果 |
|---|---|
| backend: mvn clean compile |  |
| backend: mvn test |  |
| frontend: npm run typecheck |  |
| frontend: npm run build |  |

## 4. 业务联调

| 场景 | 结果 | 说明 |
|---|---|---|
| Login |  |  |
| Dashboard |  |  |
| Project |  |  |
| Member |  |  |
| Repository |  |  |
| Task Execute |  |  |
| Chat SSE |  |  |
| Knowledge / RAG |  |  |
| Observability |  |  |

## 5. 修复问题

- 

## 6. 已知限制

- 

## 7. 结论

是否具备本地发布/演示条件：

- [ ] 是
- [ ] 否
```
