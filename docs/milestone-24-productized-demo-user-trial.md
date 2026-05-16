# Milestone 24: 产品化 Demo 与真实用户试用

## 1. 背景

当前项目已经完成从基础后端、前端控制台、真实模型网关、GitHub OAuth / PR Review、Docker / CI/CD、生产部署、监控告警与安全加固的完整链路。

系统现在已经具备：

- 本地开发环境可运行。
- Docker Compose 演示环境可运行。
- 生产单机部署文档和脚本已就绪。
- Chat / RAG / Task / Agent / Model Gateway / GitHub PR Review 主链路可演示。
- 前端已经完成科技感 UI 升级和一致性 Polish。
- 运维、备份、日志、安全检查脚本已补齐。

Milestone 24 的目标不是继续堆功能，而是把项目整理成一个“新用户可以直接试用、演示人员可以稳定讲解、试用反馈可以被收集”的产品化 Demo。

> 验收目标：一个没有读过 README 的试用者，登录后能在 10 分钟内完成 Project → Knowledge → Chat → Task Execute → Model Usage / Audit 的核心体验，并且知道下一步怎么反馈问题。

## 2. 严格边界

执行本阶段必须遵守：

1. 不重写前端工程。
2. 不更换技术栈。
3. 不改核心业务逻辑。
4. 不引入真实付费模型调用作为默认行为。
5. 不要求真实 GitHub OAuth 才能完成 Demo。
6. 不做 Git 写操作。
7. 不执行危险 shell / Docker 清理命令。
8. 不提交 `.env` / `.env.production` / API Key / OAuth Secret。
9. 不破坏已有 E2E、Docker、生产脚本和 CI。
10. 不把 Demo 模式做成后门，不跳过后端权限校验。
11. 不新增复杂多租户、计费、组织管理、邮件系统。
12. 不做移动端深度适配。

允许做：

- Demo 数据脚本。
- Demo 模式文案与视觉提示。
- Onboarding 引导。
- 示例数据和示例文档。
- 用户试用反馈模板。
- Demo walkthrough 文档。
- 小范围前端交互优化。
- 小范围后端只读 / Demo 安全配置，但必须向后兼容。

## 3. 总目标

实现 6 个能力：

1. Demo 数据一键初始化
   - 创建 Demo Project。
   - 创建 Demo Knowledge Base。
   - 上传 Demo Document。
   - 创建 Demo Chat Session。
   - 创建 Demo Task。
   - 预置可展示的 Agent / Model Gateway / Audit / Usage 数据。
   - 脚本必须幂等，可以重复运行。

2. Demo 体验引导
   - 登录后能看到 Demo 环境状态。
   - Dashboard 展示推荐体验路径。
   - 项目详情页提示下一步操作。
   - Chat / Knowledge / Task 页面给出轻量提示。

3. Demo 安全边界
   - 明确标识当前为 Demo / MOCK。
   - 不误导用户以为已经接入真实生产模型。
   - GitHub 写操作继续禁止。
   - 模型成本、provider、fallback 状态清晰可见。

4. 演示讲解材料
   - 5 分钟快速 Demo 脚本。
   - 15 分钟深度 Demo 脚本。
   - 真实用户试用清单。
   - 常见问题和故障处理。

5. 反馈收集
   - 提供用户反馈模板。
   - 提供试用记录模板。
   - 明确反馈分类：Bug、体验、性能、模型效果、业务价值、部署问题。

6. 试用验收
   - 提供 Demo smoke test。
   - 提供人工验收清单。
   - 明确通过 / 失败标准。

## 4. 执行前必须阅读

执行前先完整阅读：

```text
README.md
docs/demo-data-guide.md
docs/frontend-smoke-test-plan.md
docs/release-qa-report.md
docs/testing-strategy.md
docs/production-deployment-runbook.md
docs/production-observability-runbook.md
docs/model-provider-production-setup.md
docs/github-oauth-production-setup.md
frontend/src/modules/dashboard/pages/DashboardPage.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/knowledge/pages/KnowledgeBasePage.vue
frontend/src/modules/task/pages/TaskListPage.vue
frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/model/pages/ModelConfigPage.vue
frontend/src/modules/admin/pages/ObservabilityPage.vue
scripts/dev-seed-demo-data.sh
scripts/backend-unified-smoke-test.sh
scripts/release-demo-check.sh
```

如果某些文件不存在，先说明实际情况，再选择最小可行替代方案。

## 5. 建议新增 / 修改文件

### 5.1 文档

新增：

```text
docs/demo-walkthrough.md
docs/user-feedback-template.md
docs/demo-acceptance-checklist.md
docs/milestone-24-validation-report-template.md
```

修改：

```text
README.md
docs/demo-data-guide.md
docs/frontend-smoke-test-plan.md
docs/release-qa-report.md
```

要求：

- 文档面向真实试用者和演示者，不只面向开发者。
- 每个步骤都要给出入口、预期看到的页面、失败时怎么处理。
- 不要要求用户理解数据库、Flyway、MyBatis-Plus 等内部细节。

### 5.2 脚本

新增或增强：

```text
scripts/demo-seed-data.sh
scripts/demo-reset-data.sh
scripts/demo-smoke-test.sh
```

如果已有 `scripts/dev-seed-demo-data.sh`，优先增强它或封装它，不要复制大量重复逻辑。

脚本要求：

- 使用 `set -euo pipefail`。
- 输出清晰 `PASS` / `WARN` / `FAIL` / `SKIP`。
- 支持从环境变量读取：
  - `BASE_URL`
  - `ADMIN_EMAIL`
  - `ADMIN_PASSWORD`
  - `DEMO_PROJECT_NAME`
- 不打印 token、password、API Key。
- 幂等执行。
- 删除 / reset 脚本必须有显式确认参数，例如 `--yes`。

### 5.3 前端可选增强

允许小范围修改：

```text
frontend/src/modules/dashboard/pages/DashboardPage.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/knowledge/pages/KnowledgeBasePage.vue
frontend/src/modules/task/pages/TaskListPage.vue
frontend/src/modules/model/pages/ModelConfigPage.vue
frontend/src/modules/admin/pages/ObservabilityPage.vue
frontend/src/shared/components/*
```

建议新增：

```text
frontend/src/shared/components/DemoBadge.vue
frontend/src/shared/components/DemoGuidePanel.vue
frontend/src/shared/components/WalkthroughStep.vue
```

限制：

- 不改 API client 的既有行为。
- 不改 auth store 的 token 逻辑。
- 不破坏 Chat SSE。
- 不破坏 FloatingDock / DynamicWorkspace 视觉体系。
- 不引入新的 UI 框架。
- 不把大段说明文字塞进页面，保持控制台风格。

### 5.4 后端可选增强

原则上不需要新增后端功能。若确实需要，可只做以下轻量增强：

- 新增 Demo profile 配置项。
- 新增只读的 `/api/demo/status`。
- 新增 Demo seed 仅内部脚本使用的服务，但默认不暴露公网。

限制：

- 不绕过认证。
- 不开放危险 reset API。
- 不让前端调用后端清库接口。
- 不改变核心业务表结构。

## 6. Demo 数据要求

Demo 数据必须覆盖核心体验路径。

### 6.1 Demo Project

建议名称：

```text
Demo AI Workspace
```

内容：

- description：说明这是演示项目。
- techStack：Java、Vue、Spring Boot、RAG、AI Agent。
- owner：admin。
- status：ACTIVE。

### 6.2 Demo Knowledge Base

建议名称：

```text
Product Knowledge Base
```

至少包含 2-3 个文档：

1. `AI Coding Platform Overview`
   - 说明平台能力。
   - 包含 Chat、Task、RAG、Model Gateway 关键词。

2. `Agent Workflow Guide`
   - 说明 Agent Orchestrator 执行流程。
   - 包含 Task、Execution、Artifact、Model Log。

3. `Repository Review Guide`
   - 说明 GitHub PR Review 的只读流程。
   - 不包含真实 token。

### 6.3 Demo Chat Session

建议创建一个会话：

```text
Ask Product Knowledge
```

预置或引导用户发送：

```text
请总结这个平台如何把 RAG、Agent 和任务执行串起来。
```

期望：

- Chat 能返回流式消息。
- 如果有 RAG 数据，references 能显示。
- 如果没有真实模型，Mock/Fallback 状态要可理解。

### 6.4 Demo Task

建议创建一个任务：

```text
Generate architecture review summary
```

内容：

- taskType：FEATURE 或 REVIEW。
- priority：MEDIUM。
- agent：Backend Agent 或 Architect Agent。
- instruction：让 Agent 根据知识库生成架构评审摘要。

期望：

- 可执行。
- 可查看 logs。
- 可查看 artifacts。
- 可查看 executions 和 model logs。

### 6.5 Demo Observability

演示后应能看到：

- audit logs。
- model usage summary。
- model request logs。
- task execution records。

## 7. Demo 页面体验要求

### 7.1 Dashboard

需要展示：

- 当前环境状态：Demo / Local / Production。
- 推荐体验路径：
  1. Open Demo Project
  2. Explore Knowledge Base
  3. Ask Chat with RAG
  4. Execute Agent Task
  5. Review Usage & Audit
- 如果没有 Demo 数据，显示“Run demo seed script”提示。

不要：

- 把 Dashboard 做成营销 Landing Page。
- 放大段说明文字。
- 改成卡片堆叠的单调页面。

### 7.2 Project Detail

需要让用户一眼知道：

- 当前项目是否是 Demo 项目。
- 6 个 Tab 分别能演示什么。
- 下一步建议去哪里。

### 7.3 Chat

需要：

- 保持 SSE 流式输出。
- 保持 references 展示。
- 清晰显示 RAG 是否启用。
- 错误时说明是网络 / 认证 / 模型 fallback / RAG 无结果。

### 7.4 Knowledge

需要：

- 明确文档数量、chunk 数量。
- RAG 搜索结果可读。
- Chunk preview 可读。

### 7.5 Task

需要：

- 任务状态清晰。
- 执行按钮清晰。
- Logs / Artifacts / Executions / Model Logs 可快速找到。

### 7.6 Observability

需要：

- Demo 后能快速看到关键数据。
- Audit logs 筛选可用。
- Model usage / cost 面板可理解。

## 8. Demo 文档要求

### 8.1 demo-walkthrough.md

新增：

```text
docs/demo-walkthrough.md
```

必须包含：

1. Demo 前准备
   - 后端启动。
   - 前端启动。
   - Demo 数据初始化。
   - 登录账号。

2. 5 分钟快速 Demo
   - 登录。
   - 打开 Demo Project。
   - Chat 提问。
   - 查看 references。
   - 执行 Task。
   - 查看 logs / artifacts。
   - 查看 observability。

3. 15 分钟深度 Demo
   - Knowledge 文档上传。
   - RAG 搜索。
   - Model Gateway provider 状态。
   - GitHub 页面说明。
   - Audit / Usage / Security 说明。

4. 常见失败处理
   - 登录失败。
   - 后端未启动。
   - Chat SSE 失败。
   - 没有 Demo 数据。
   - Model provider 未配置。
   - GitHub OAuth 未配置。

### 8.2 user-feedback-template.md

新增：

```text
docs/user-feedback-template.md
```

必须包含：

- 试用者信息。
- 使用场景。
- 完成路径。
- 卡住的位置。
- 最有价值的功能。
- 最困惑的地方。
- 性能感受。
- UI 感受。
- 模型回答质量。
- 是否愿意继续使用。
- Bug 记录模板。
- 截图 / 录屏链接。

### 8.3 demo-acceptance-checklist.md

新增：

```text
docs/demo-acceptance-checklist.md
```

必须包含：

- 环境检查。
- 数据检查。
- 主链路检查。
- 负向检查。
- 安全检查。
- 体验检查。
- 通过 / 阻塞标准。

### 8.4 milestone-24-validation-report-template.md

新增：

```text
docs/milestone-24-validation-report-template.md
```

用于执行完成后填写验收结果。

## 9. Demo Smoke Test 要求

新增：

```text
scripts/demo-smoke-test.sh
```

至少检查：

1. Frontend 首页可访问。
2. Login 成功。
3. `/api/auth/me` 成功。
4. Project list 非空，且包含 Demo Project。
5. Knowledge Base list 非空。
6. RAG search 能返回结果或可解释的空结果。
7. Chat session 创建成功。
8. Chat send message 成功。
9. Task create 成功。
10. Task execute 成功或返回可解释状态。
11. Observability overview 可访问。
12. Audit logs 可访问。
13. No token 访问受保护 API 返回 401 / 403。

脚本输出示例：

```text
[PASS] Frontend reachable
[PASS] Login admin
[PASS] Demo Project exists
[PASS] Chat message sent
[WARN] Real model provider not configured, using MOCK
[PASS] No-token request rejected
```

## 10. Demo Reset 要求

新增：

```text
scripts/demo-reset-data.sh
```

要求：

- 默认拒绝执行。
- 必须传 `--yes`。
- 只删除 Demo 前缀 / Demo 标记的数据。
- 不删除真实用户数据。
- 删除前输出将影响的数据类型。
- 不执行 `DROP DATABASE`。

如果当前数据结构无法安全区分 Demo 数据，则不要实现真实删除逻辑，只输出“manual reset instructions”，并在报告中说明原因。

## 11. 验证要求

完成后必须执行：

### 11.1 后端

```bash
cd backend
mvn test
```

如果改了后端代码，再执行：

```bash
mvn clean compile
```

### 11.2 前端

如果改了前端代码，必须执行：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

### 11.3 脚本

必须执行：

```bash
bash -n scripts/demo-seed-data.sh
bash -n scripts/demo-reset-data.sh
bash -n scripts/demo-smoke-test.sh
```

如果本地后端 / 前端正在运行，继续执行：

```bash
bash scripts/demo-smoke-test.sh
```

### 11.4 文档检查

必须确认：

```bash
test -f docs/demo-walkthrough.md
test -f docs/user-feedback-template.md
test -f docs/demo-acceptance-checklist.md
test -f docs/milestone-24-validation-report-template.md
```

## 12. 人工验收清单

手动验证：

1. 登录成功。
2. Dashboard 能看到 Demo 引导。
3. Demo Project 可打开。
4. Knowledge Base 有 Demo 文档。
5. RAG Search 有可理解结果。
6. Chat 可以发送消息。
7. Chat SSE 正常流式输出。
8. Chat references 正常展示。
9. Task 可以创建。
10. Task 可以执行。
11. Task logs / artifacts / executions / model logs 可查看。
12. Model Gateway 页面可说明当前 Mock / Real Provider 状态。
13. Observability 能看到 audit / usage。
14. 无 token 请求被拒绝。
15. Logout 正常。

## 13. 完成后输出格式

完成后必须按以下格式输出：

```text
Milestone 24 完成报告

1. 新增/修改文件清单
2. Demo 数据初始化实现
3. Demo Reset / Smoke Test 脚本说明
4. Dashboard / Onboarding 体验说明
5. Demo Walkthrough 文档说明
6. 用户反馈模板说明
7. 安全边界与 Mock / Real Provider 提示
8. 自动化验证结果
9. 手动浏览器验证结果
10. 已知限制
11. 是否可以进入 Milestone 25
```

## 14. 不做事项

本阶段明确不做：

- 不接真实邮件。
- 不实现邀请邮件发送。
- 不实现真实支付 / 订阅 / 计费。
- 不实现组织 / 团队多租户。
- 不实现公开注册。
- 不实现完整帮助中心。
- 不实现客服系统。
- 不实现线上埋点平台。
- 不引入 Sentry / PostHog / Amplitude。
- 不引入 Kubernetes。

## 15. Claude 执行提示词

下面这段可以直接复制给 Claude：

```text
请根据项目中的文档执行 Milestone 24。

文档路径：
docs/milestone-24-productized-demo-user-trial.md

执行要求：
1. 先完整阅读该文档，再检查当前项目结构。
2. 本阶段目标是把现有系统整理成可演示、可试用、可收集反馈的产品化 Demo，不是继续堆新业务功能。
3. 不要重写前端工程，不要更换技术栈，不要更换 UI 框架。
4. 不要改 Auth、Project、Member、Repository、Task、Agent、Chat、RAG、Model Gateway、GitHub、Observability 已验证通过的核心逻辑，除非 Demo 体验必须依赖，并且修改前要说明原因。
5. 不接真实大模型作为默认行为，不要求真实 GitHub OAuth 才能完成 Demo。
6. 不做 Git 写操作，不做真实危险 reset，不执行 DROP DATABASE，不清理 Docker 全局资源。
7. 不提交 .env、.env.production、API Key、OAuth Secret、数据库 dump、日志、备份文件。
8. Demo 数据脚本必须幂等，Reset 脚本必须需要 --yes，并且只允许处理 Demo 数据。
9. 前端如需修改，只做小范围产品化体验增强，保持当前 DynamicWorkspace / FloatingDock / dark tech console 风格。
10. Chat SSE 不能破坏，RAG references 不能破坏，Task Execute 状态机不能绕过。
11. 所有新增文档要面向真实试用者和演示人员，步骤清晰、可执行、可排障。
12. 所有脚本使用 set -euo pipefail，不打印 token、password、API Key。

需要实现：
1. 新增或增强 Demo 数据初始化脚本：scripts/demo-seed-data.sh。
2. 新增 Demo reset 脚本：scripts/demo-reset-data.sh，必须 --yes，且不能删除非 Demo 数据。
3. 新增 Demo smoke test：scripts/demo-smoke-test.sh。
4. 新增 docs/demo-walkthrough.md，包含 5 分钟快速 Demo 和 15 分钟深度 Demo。
5. 新增 docs/user-feedback-template.md。
6. 新增 docs/demo-acceptance-checklist.md。
7. 新增 docs/milestone-24-validation-report-template.md。
8. 更新 README.md 和 docs/demo-data-guide.md，把 Demo 启动、数据初始化、试用路径串起来。
9. 如有必要，小范围优化 Dashboard / Project Detail / Chat / Knowledge / Task / Observability 的 Demo 引导，但不要改变业务逻辑。
10. 明确 Mock / Real Provider / GitHub OAuth 未配置时的展示与文档说明，避免误导试用者。

完成后必须执行：
1. bash -n scripts/demo-seed-data.sh
2. bash -n scripts/demo-reset-data.sh
3. bash -n scripts/demo-smoke-test.sh
4. cd backend && mvn test
5. 如果改了后端代码：cd backend && mvn clean compile
6. 如果改了前端代码：cd frontend && npm run typecheck && npm run build && npm run test:e2e -- --workers=1
7. 如果本地前后端正在运行：bash scripts/demo-smoke-test.sh

完成后按以下格式输出：
1. 新增/修改文件清单
2. Demo 数据初始化实现
3. Demo Reset / Smoke Test 脚本说明
4. Dashboard / Onboarding 体验说明
5. Demo Walkthrough 文档说明
6. 用户反馈模板说明
7. 安全边界与 Mock / Real Provider 提示
8. 自动化验证结果
9. 手动浏览器验证结果
10. 已知限制
11. 是否可以进入 Milestone 25

现在开始实现，不要只给计划。
```
