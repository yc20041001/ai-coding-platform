# Milestone 27: E2E 稳定性修复与发布质量收口

## 1. 背景

当前项目已经完成：

- Milestone 24：产品化 Demo 与真实用户试用。
- Milestone 25：反馈闭环与产品迭代管理。
- Milestone 26：产品官网 / 对外展示与试用入口。

现在系统已经具备：

- 可演示的控制台。
- 公开展示页。
- Demo 数据和试用文档。
- GitHub Issue / PR / Release 模板。
- Docker / CI / 生产部署 / 监控文档。

但最近多个 Milestone 验证报告中都出现同一个已知问题：

```text
frontend/e2e/project-task-chat.spec.ts
project creation dialog timeout
```

这说明项目功能已经基本齐全，但自动化验收存在不稳定点。Milestone 27 的目标是专门做 E2E 稳定性修复和发布质量门收口，让每次发版前的自动检查更可靠。

> 验收目标：前端 E2E 在顺序执行模式下稳定 12/12 通过；release-check 脚本能明确阻止已知质量问题进入发布。

## 2. 严格边界

执行本阶段必须遵守：

1. 不新增业务功能。
2. 不改后端核心业务逻辑。
3. 不重写前端页面。
4. 不更换测试框架。
5. 不删除有价值的 E2E 用例。
6. 不通过扩大 timeout 掩盖真实问题，除非有明确原因并说明。
7. 不把失败用例直接 skip，除非同时创建跟踪文档并说明阻塞原因。
8. 不依赖手动点击才能通过测试。
9. 不依赖测试执行顺序中的脏数据。
10. 不提交 `.env`、token、API Key、数据库 dump、视频、trace 包。
11. 不破坏 Chat SSE。
12. 不破坏 Milestone 26 的 `/public` 公开路由。

允许做：

- 为关键元素补 `data-testid`。
- 优化 Playwright selectors。
- 优化等待条件。
- 拆分不稳定测试。
- 增加测试数据唯一命名。
- 增加测试清理逻辑。
- 增强 release-check 脚本。
- 新增测试稳定性文档。
- 小范围修复确认为前端 UI 时序问题的代码。

## 3. 总目标

实现 6 个能力：

1. E2E flaky 根因定位
   - 明确当前 `project-task-chat.spec.ts` 失败原因。
   - 区分测试选择器问题、UI 时序问题、数据污染问题、后端响应问题。

2. 选择器稳定化
   - 关键按钮、表单、Dialog、Tab、Drawer 使用 `data-testid`。
   - 减少依赖中文 / 英文文案和 CSS 类名的选择器。

3. 测试数据隔离
   - 使用唯一项目名 / 任务名 / 会话名。
   - 避免依赖已有 Demo 数据。
   - 可重复运行。

4. 测试等待策略
   - Dialog 打开等待可见。
   - API 完成后再断言 UI。
   - 路由跳转后等待关键元素。
   - SSE 流等待 done / content 状态，不靠固定 sleep。

5. Release Gate 收口
   - `scripts/release-checklist.sh` 或现有检查脚本纳入 E2E 结果。
   - 明确哪些失败阻塞发布。
   - 明确哪些 WARN 可接受。

6. 测试文档化
   - 记录如何运行。
   - 记录常见失败和修复方式。
   - 记录本地 / Docker / CI 的差异。

## 4. 执行前必须阅读

执行前先阅读：

```text
frontend/playwright.config.ts
frontend/e2e/auth.spec.ts
frontend/e2e/project-task-chat.spec.ts
frontend/e2e/knowledge-observability.spec.ts
frontend/e2e/model-gateway.spec.ts
frontend/src/modules/auth/pages/LoginPage.vue
frontend/src/modules/project/pages/ProjectListPage.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/modules/task/pages/TaskListPage.vue
frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/shared/api/client.ts
frontend/src/shared/utils/sse.ts
scripts/release-checklist.sh
scripts/run-frontend-checks.sh
docs/frontend-smoke-test-plan.md
docs/testing-strategy.md
docs/release-qa-report.md
```

如果某些文件不存在，先说明实际情况，再选择最小可行替代方案。

## 5. 需要重点修复的问题

### 5.1 当前已知失败

已知失败：

```text
frontend/e2e/project-task-chat.spec.ts
project creation dialog timeout
```

需要确认：

1. 是创建项目按钮没点到？
2. 是 Dialog 没打开？
3. 是 Dialog 打开但测试找不到元素？
4. 是按钮文案改成英文后选择器不匹配？
5. 是 Element Plus 动画导致等待太短？
6. 是后端 API 返回慢？
7. 是数据重名导致创建失败？
8. 是登录态未准备好？

不要直接把 timeout 调大作为唯一修复。

### 5.2 验收标准

必须达到：

```bash
cd frontend
npm run test:e2e -- --workers=1
```

稳定结果：

```text
12 passed
```

至少连续运行 2 次通过：

```bash
npm run test:e2e -- --workers=1
npm run test:e2e -- --workers=1
```

如果环境原因无法连续两次运行，必须说明原因。

## 6. 选择器规范

### 6.1 推荐 data-testid

建议补齐：

```text
data-testid="login-email"
data-testid="login-password"
data-testid="login-submit"
data-testid="btn-create-project"
data-testid="dialog-create-project"
data-testid="input-project-name"
data-testid="input-project-description"
data-testid="btn-submit-project"
data-testid="project-table"
data-testid="project-row"
data-testid="project-detail-title"
data-testid="tab-tasks"
data-testid="tab-chat"
data-testid="btn-create-task"
data-testid="dialog-create-task"
data-testid="btn-submit-task"
data-testid="btn-execute-task"
data-testid="task-table"
data-testid="chat-session-list"
data-testid="chat-message-input"
data-testid="btn-send-message"
```

要求：

- 不要为了测试破坏 DOM 结构。
- 不要把 `data-testid` 用于样式。
- 不要在业务逻辑中读取 `data-testid`。
- 优先加在稳定交互元素上。

### 6.2 Playwright Selector 规则

优先级：

1. `page.getByTestId()`
2. `page.getByRole()`
3. `page.getByLabel()`
4. `page.getByText()`
5. CSS selector

尽量避免：

- `.nth()` 选择不稳定项。
- 依赖 Element Plus 内部类名。
- 依赖动画中间状态。
- 依赖中文 / 英文文案。

## 7. 测试数据规范

### 7.1 唯一命名

所有测试创建的数据必须带唯一后缀：

```ts
const suffix = Date.now().toString()
const projectName = `E2E Project ${suffix}`
const taskTitle = `E2E Task ${suffix}`
const chatTitle = `E2E Chat ${suffix}`
```

### 7.2 不依赖 Demo 数据

E2E 可以使用 admin 登录，但不要依赖：

- Demo AI Workspace 必须存在。
- Product Knowledge Base 必须存在。
- Ask Product Knowledge 必须存在。

Demo 测试应放在单独 smoke test，而不是主 E2E 必须条件。

### 7.3 清理策略

如果有 DELETE API：

- 测试结束后清理本次创建的项目 / 任务。

如果没有可靠 DELETE API：

- 使用唯一命名避免冲突。
- 在文档中说明残留测试数据可接受。

## 8. 等待策略规范

### 8.1 Dialog

推荐：

```ts
await page.getByTestId('btn-create-project').click()
await expect(page.getByTestId('dialog-create-project')).toBeVisible()
```

提交后：

```ts
await Promise.all([
  page.waitForResponse(resp => resp.url().includes('/api/projects') && resp.request().method() === 'POST'),
  page.getByTestId('btn-submit-project').click(),
])
```

### 8.2 Route

推荐：

```ts
await page.waitForURL(/\/projects\/\d+/)
await expect(page.getByTestId('project-detail-title')).toBeVisible()
```

### 8.3 SSE

不要用固定 sleep 作为唯一判断。

推荐：

- 等待 streaming indicator。
- 等待 message content 出现。
- 等待 done 后的状态变化。

### 8.4 API

对关键写操作用 `waitForResponse`：

- Create Project。
- Create Task。
- Execute Task。
- Send Chat Message。

## 9. 建议修改文件

### 9.1 前端组件

可能需要修改：

```text
frontend/src/modules/project/pages/ProjectListPage.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/modules/task/pages/TaskListPage.vue
frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/auth/pages/LoginPage.vue
```

只允许添加测试标识或修复明确 UI 时序问题。

### 9.2 E2E 测试

需要修改：

```text
frontend/e2e/project-task-chat.spec.ts
frontend/e2e/auth.spec.ts
frontend/e2e/knowledge-observability.spec.ts
frontend/e2e/model-gateway.spec.ts
```

重点：

- 把文案选择器替换为 testId。
- 把固定 wait 替换为 waitForResponse / waitForURL / expect visible。
- 保持测试意图清晰。

### 9.3 脚本

可能修改：

```text
scripts/run-frontend-checks.sh
scripts/release-checklist.sh
scripts/release-demo-check.sh
```

要求：

- 明确 E2E 失败时退出非 0。
- 输出清晰失败原因。
- 不吞掉错误。

### 9.4 文档

新增：

```text
docs/e2e-stability-guide.md
docs/milestone-27-validation-report-template.md
```

修改：

```text
docs/testing-strategy.md
docs/frontend-smoke-test-plan.md
docs/release-qa-report.md
```

## 10. Release Gate 要求

发布前必须通过：

```bash
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
cd frontend && npm run test:e2e -- --workers=1
bash scripts/release-checklist.sh
```

如果某项失败：

- P0 / P1：阻塞发布。
- E2E 主链路失败：阻塞发布。
- 文档链接缺失：阻塞发布。
- 只读外部服务未配置：可 WARN，但不能 FAIL。

## 11. 验证要求

完成后必须执行：

### 11.1 前端质量门

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
npm run test:e2e -- --workers=1
```

### 11.2 后端回归

如果没有改后端代码，可以只说明未运行原因。

如果改了后端代码：

```bash
cd backend
mvn test
```

### 11.3 脚本检查

如果改了脚本：

```bash
bash -n scripts/run-frontend-checks.sh
bash -n scripts/release-checklist.sh
bash -n scripts/release-demo-check.sh
```

### 11.4 文档检查

```bash
test -f docs/e2e-stability-guide.md
test -f docs/milestone-27-validation-report-template.md
```

## 12. 浏览器人工验证

如果前端 dev server 可用，手动验证：

1. 登录成功。
2. 创建项目 Dialog 能稳定打开。
3. 项目创建成功。
4. 项目详情页能打开。
5. Tasks Tab 能打开。
6. 创建 Task Dialog 能稳定打开。
7. Task 创建成功。
8. Task Execute 正常。
9. Chat Tab 能打开。
10. Chat SSE 正常。
11. Public `/public` 仍可未登录访问。
12. 未登录访问 `/projects` 仍跳转 login。

## 13. 完成后输出格式

完成后必须按以下格式输出：

```text
Milestone 27 完成报告

1. 新增/修改文件清单
2. Flaky 根因分析
3. data-testid / Selector 稳定化说明
4. E2E 测试数据隔离说明
5. 等待策略优化说明
6. Release Gate 更新说明
7. 自动化验证结果
8. 浏览器手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 28
```

## 14. 不做事项

本阶段明确不做：

- 不增加业务功能。
- 不重写测试体系。
- 不引入 Cypress。
- 不引入视觉回归测试平台。
- 不接外部测试 SaaS。
- 不强行并行化所有 E2E。
- 不为了通过测试删除真实断言。
- 不把所有用例都改成 smoke-only。

## 15. Claude 执行提示词

下面这段可以直接复制给 Claude：

```text
请根据项目中的文档执行 Milestone 27。

文档路径：
docs/milestone-27-e2e-stability-release-quality.md

执行要求：
1. 先完整阅读该文档，再检查当前 frontend/e2e、Playwright 配置、相关 Vue 页面和 release 脚本。
2. 本阶段目标是修复 E2E flaky 并收口发布质量门，不是增加业务功能。
3. 不要改后端核心业务逻辑，不要重写前端页面，不要更换测试框架。
4. 当前重点问题是 frontend/e2e/project-task-chat.spec.ts 中 project creation dialog timeout。必须先定位根因，再修复。
5. 不要简单通过扩大 timeout 或 skip 用例掩盖问题。
6. 不要删除有价值的 E2E 断言。
7. 可以为关键按钮、Dialog、Tab、输入框补 data-testid。
8. Playwright selector 优先使用 getByTestId，其次 getByRole/getByLabel，尽量减少依赖文案和 Element Plus 内部类名。
9. 测试数据必须使用唯一命名，不依赖 Demo 数据必须存在。
10. 对 create project / create task / execute task / send chat message 等关键操作使用稳定等待策略，例如 waitForResponse、waitForURL、expect visible。
11. 不破坏 /public 未登录访问，不破坏 /login、/dashboard、/projects 等既有路由。
12. Chat SSE 不能破坏。

需要实现：
1. 定位 project-task-chat.spec.ts flaky 根因。
2. 为 Project 创建流程补必要 data-testid。
3. 为 Task 创建 / 执行流程补必要 data-testid。
4. 为 Chat 关键交互补必要 data-testid。
5. 重写或整理 project-task-chat.spec.ts 的不稳定选择器和等待逻辑。
6. 检查 auth.spec.ts、knowledge-observability.spec.ts、model-gateway.spec.ts 是否有明显脆弱 selector，必要时小范围修复。
7. 新增 docs/e2e-stability-guide.md。
8. 新增 docs/milestone-27-validation-report-template.md。
9. 如有必要，更新 scripts/run-frontend-checks.sh、scripts/release-checklist.sh 或 scripts/release-demo-check.sh，使 E2E 失败明确阻塞发布。
10. 更新 docs/testing-strategy.md 或 docs/frontend-smoke-test-plan.md，记录 E2E 稳定性规范。

完成后必须执行：
1. cd frontend && npm run typecheck
2. cd frontend && npm run build
3. cd frontend && npm run test:e2e -- --workers=1
4. cd frontend && npm run test:e2e -- --workers=1
5. test -f docs/e2e-stability-guide.md
6. test -f docs/milestone-27-validation-report-template.md
7. 如果改了脚本：bash -n scripts/run-frontend-checks.sh && bash -n scripts/release-checklist.sh && bash -n scripts/release-demo-check.sh
8. 如果改了后端代码：cd backend && mvn test

完成后按以下格式输出：
1. 新增/修改文件清单
2. Flaky 根因分析
3. data-testid / Selector 稳定化说明
4. E2E 测试数据隔离说明
5. 等待策略优化说明
6. Release Gate 更新说明
7. 自动化验证结果
8. 浏览器手动验证结果
9. 已知限制
10. 是否可以进入 Milestone 28

现在开始实现，不要只给计划。
```
