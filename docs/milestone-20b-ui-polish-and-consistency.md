# Milestone 20B: UI Polish 与一致性收口

## 1. 背景

Milestone 20A 已经完成第二轮 UI 视觉升级：

- 保留无左侧侧边栏的工作台结构。
- 引入 `TopCommandBar` + `FloatingDock`。
- 新增 `DynamicWorkspace`、`SignalStrip`、`StatusPulse`、`NeonDivider`、`SectionRail`、`DataOrbit` 等动态科技感组件。
- 主要页面已经切换为深色 AI Coding Console 风格。
- 20A Polish 修复轮已经处理：
  - Chat SSE 完成后误报 `STREAM_ERROR` 的问题。
  - 默认白色 `EmptyState` 与暗色 UI 冲突的问题。
  - 低高度窗口底部 `FloatingDock` 遮挡内容的问题。
  - Model Gateway / Repository / Members 部分按钮与标签白块感问题。
  - Members 加入时间换行难看的问题。
  - Playwright 支持 `PLAYWRIGHT_BASE_URL` 覆盖。

Milestone 20B 的目标是：

> 在不改变业务逻辑、不改后端接口、不破坏已通过测试的前提下，对前端 UI 做一次一致性收口，让所有页面、弹窗、抽屉、表格、空状态、loading、错误状态、按钮和文本风格形成统一的企业级科技控制台体验。

## 2. 严格边界

执行本阶段必须遵守：

1. 只改前端 UI、样式、展示文案和交互细节。
2. 不改后端接口。
3. 不改业务流程和业务判断。
4. 不改认证逻辑、权限逻辑、Token 存储逻辑。
5. 不重写前端工程。
6. 不更换 Vue、Vite、Element Plus、Pinia、Router。
7. 不新增真实模型调用。
8. 不新增 Git 写操作、Shell 执行能力或真实代码生成能力。
9. 不引入大型 3D/图表/动画依赖。
10. 不做 landing page、营销页、暗色/亮色主题切换、国际化、多语言系统。
11. 不破坏 Chat SSE。
12. 不破坏登录、项目、任务、Chat、Knowledge、Repository、Members、GitHub、Model Gateway、Observability 已有功能。
13. 不把 `dist/`、`target/`、`.env`、`.DS_Store`、截图缓存等产物提交。

## 3. 本阶段重点

20B 不是继续大改视觉方向，而是“打磨一致性”。

优先处理：

1. Dialog / Drawer 暗色统一。
2. 表格、分页、空状态、loading、error 状态统一。
3. Chat 页面输入区、消息气泡、references、SSE 状态细节优化。
4. Project Detail 内部 6 个 Tab 的视觉一致性。
5. Repository / Members / Knowledge / Model Gateway / GitHub 页面残留白块和中英混杂文案清理。
6. 低高度窗口底部 Dock 遮挡回归检查。
7. 前端 E2E 对英文按钮和动态布局的兼容性增强。
8. Bundle warning 初步优化或记录明确后续处理方案。

## 4. 需要检查的现有文件

执行前先阅读：

```text
docs/milestone-20a-ui-visual-upgrade-phase-2.md
docs/frontend-smoke-test-plan.md
frontend/src/styles/index.css
frontend/src/styles/element.css
frontend/src/app/layouts/BasicLayout.vue
frontend/src/shared/components/TopCommandBar.vue
frontend/src/shared/components/FloatingDock.vue
frontend/src/shared/components/DynamicWorkspace.vue
frontend/src/shared/components/EmptyState.vue
frontend/src/shared/components/ErrorState.vue
frontend/src/shared/components/LoadingState.vue
frontend/src/shared/components/GlowButton.vue
frontend/src/shared/components/PageHeader.vue
frontend/src/shared/utils/sse.ts
frontend/playwright.config.ts
```

重点页面：

```text
frontend/src/modules/dashboard/pages/DashboardPage.vue
frontend/src/modules/project/pages/ProjectListPage.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/modules/task/pages/TaskListPage.vue
frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/knowledge/pages/KnowledgeBasePage.vue
frontend/src/modules/repository/pages/RepositoryPanel.vue
frontend/src/modules/member/pages/MemberPanel.vue
frontend/src/modules/model/pages/ModelConfigPage.vue
frontend/src/modules/github/pages/GithubIntegrationPage.vue
frontend/src/modules/github/pages/PullRequestReviewPage.vue
frontend/src/modules/admin/pages/ObservabilityPage.vue
frontend/src/modules/agent/pages/AgentListPage.vue
```

## 5. 具体任务

### 5.1 Dialog / Drawer 暗色统一

目标：

所有 `el-dialog`、`el-drawer`、表单弹窗、详情抽屉都应符合 20A 深色科技风格。

要求：

- 背景使用 `var(--app-surface)` 或 `var(--app-panel)`。
- 边框使用 `var(--app-border)` / `var(--app-border-strong)`。
- 标题、label、placeholder、footer 按钮颜色统一。
- 不出现大面积白底。
- Dialog footer 按钮不应突兀成默认白按钮。
- Drawer 内 Markdown / JSON / logs 内容应可读。

重点检查：

```text
TaskDetailPage.vue
AgentExecutionDrawer.vue
RepositoryPanel.vue
MemberPanel.vue
KnowledgeBasePage.vue
ModelConfigPage.vue
PullRequestReviewPage.vue
ObservabilityPage.vue
```

### 5.2 表格体验统一

目标：

所有表格保持暗色、可扫读、行 hover 有轻微能量条或边框提示。

要求：

- 表头颜色统一。
- 表格行 hover 不要过亮。
- 空表格不要同时出现 Element Plus 默认 `No Data` 和自定义 `EmptyState` 的重复空状态。若保留 Element 表格内空态，应统一样式；若使用外部 `EmptyState`，表格内部空态尽量隐藏或弱化。
- 分页按钮暗色化，当前页样式清晰。
- 长时间字段不换行。
- 操作按钮避免过度拥挤。

重点检查：

```text
ProjectListPage.vue
TaskListPage.vue
RepositoryPanel.vue
MemberPanel.vue
KnowledgeBasePage.vue
ModelConfigPage.vue
ObservabilityPage.vue
AgentListPage.vue
```

### 5.3 Empty / Loading / Error 状态统一

目标：

所有页面状态都像同一套产品，而不是 Element Plus 默认状态和自定义状态混杂。

要求：

- `EmptyState` 使用统一图标、标题、描述。
- `LoadingState` 统一深色 spinner / skeleton / loading 文案。
- `ErrorState` 统一标题、描述、retry 按钮。
- 页面级 loading 不应遮挡顶部导航。
- 表格 loading 不应出现突兀白色遮罩。

建议：

- 在 `element.css` 中统一 `v-loading` mask 背景。
- 检查所有 `EmptyState description="暂无..."` 并替换为英文或当前页面统一文案。

### 5.4 Chat 页面精修

目标：

Chat 是当前产品展示的核心体验，需要更稳定、更有质感。

要求：

- SSE token 流结束后不能误报 network error。
- 离开页面时必须 abort 当前 stream。
- Streaming 状态应清晰但不刺眼。
- 输入区在低高度窗口不能被 Dock 遮挡。
- references 展示要紧凑、清晰，可展开 snippet。
- 用户消息与 Agent 消息视觉区分明确。
- 空会话状态、无消息状态、发送中状态统一。
- 不改变 Chat API、不改变 SSE 协议。

重点检查：

```text
frontend/src/shared/utils/sse.ts
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/chat/components/ReferenceList.vue
```

### 5.5 Project Detail Tabs 一致性

目标：

Project Detail 下的 6 个 Tab 不应像 6 个不同产品拼起来。

需要检查：

```text
Overview
Tasks
Chat
Knowledge
Repository
Members
```

要求：

- SectionRail active 状态一致。
- 每个 Tab 顶部标题、描述、操作区风格一致。
- 内部 `PageHeader` 与 `DynamicWorkspace` 不要层级冲突。
- Repository / Members 的中文文案尽量清理。
- Bottom dock 不遮挡 Tab 底部操作。

### 5.6 Model Gateway 页面收口

目标：

Model Gateway 是 AI 基础设施页面，应保持最强科技感之一。

要求：

- Provider card 内的模型 tag 不应是白底。
- Test Connection 按钮风格统一。
- Config table 空状态统一。
- Cost panel 与 provider cards 间距一致。
- Dialog 表单暗色统一。
- 文案统一英文。

### 5.7 GitHub / PR Review 页面收口

目标：

GitHub Integration 和 PR Review 页面属于 DevOps / Code Quality 场景，需要更像专业代码工作台。

要求：

- OAuth 未配置、未绑定、已绑定状态视觉统一。
- Repo cards / PR cards hover 态清晰。
- Diff 预览可读，代码块背景不突兀。
- Review findings 风险等级颜色统一。
- 不调用 GitHub 写接口，不改变 OAuth 逻辑。

### 5.8 Observability 页面收口

目标：

Observability 页面应保持监控台质感，数据密度高但不杂乱。

要求：

- Audit filter 表单暗色统一。
- 日期选择器样式与暗色背景兼容。
- 表格列宽合理，不出现明显换行割裂。
- Overview / Model Usage / Audit Logs 分区清晰。

### 5.9 中英混杂文案清理

目标：

20A 后多数主页面已偏英文，但仍有少量中文残留。本阶段做一次局部清理。

原则：

- 前端控制台主 UI 使用英文。
- 后端返回的业务错误暂不强行翻译。
- README / docs 可保留中文。
- 登录页演示账号说明可保留中文或改为英文，但要统一。

建议检查：

```bash
cd frontend
rg "[\u4e00-\u9fff]" src
```

允许保留：

- 后端错误透传。
- 测试数据中的中文。
- 明确业务需要的中文内容。

### 5.10 E2E 与测试适配

目标：

UI 文案和布局变化后，E2E 不应依赖脆弱中文按钮文本。

要求：

- 优先使用 `data-testid`。
- 对已英文化按钮，更新测试断言。
- `playwright.config.ts` 保持支持 `PLAYWRIGHT_BASE_URL`。
- 不为了测试牺牲产品 UI。

必须验证：

```bash
cd frontend
npm run typecheck
npm run build
PLAYWRIGHT_BASE_URL=http://localhost:5175 npm run test:e2e -- --workers=1
```

如没有启动 `5175`，可使用：

```bash
cd frontend
npm run dev -- --host 0.0.0.0 --port 5175
```

也可以验证 Docker 静态页：

```bash
docker compose -f deploy/docker-compose.app.yml up -d --build frontend
npm run test:e2e -- --workers=1
```

## 6. 不要做的事

不要：

- 重构 API client。
- 重构 Pinia store。
- 改后端 Controller / Service。
- 改数据库迁移。
- 改权限模型。
- 改 Docker Compose 服务拓扑。
- 改 GitHub Actions 主流程，除非 E2E 端口配置明显需要。
- 增加新的业务模块。
- 加真实模型 Key。
- 加大型 UI 框架或图表库。

## 7. 验收标准

### 7.1 自动化验收

必须通过：

```bash
cd frontend
npm run typecheck
npm run build
PLAYWRIGHT_BASE_URL=http://localhost:5175 npm run test:e2e -- --workers=1
```

建议额外确认：

```bash
cd backend
mvn test
```

如果只改前端 UI，后端测试可说明未运行原因。

### 7.2 浏览器手动验收

使用 `http://localhost:5175` 或 Docker 的 `http://localhost:5173` 检查：

1. Login 页面可登录。
2. Dashboard 不被 Dock 遮挡。
3. Projects 列表无白块空态。
4. Project Detail 的 6 个 Tab 都能切换。
5. Repository Tab 底部 Diff 操作不被 Dock 遮挡。
6. Members Joined 时间不换行，按钮风格统一。
7. Tasks 创建、执行入口可见。
8. Task Detail Drawer / Logs / Artifacts / Executions 可读。
9. Chat SSE 流式回复正常，不误报 network error。
10. Chat references 展示正常。
11. Knowledge 文档列表、chunk preview、RAG search 显示正常。
12. Model Gateway provider cards 不再出现白色 tag / 白色按钮。
13. GitHub Integration 状态展示正常。
14. PR Review 页面 Diff / findings 可读。
15. Observability filter / table / metric panel 风格统一。
16. Logout 正常。

## 8. 完成报告格式

完成后按以下格式输出：

```text
Milestone 20B 完成报告

1. 新增/修改文件清单
2. Dialog / Drawer 暗色统一
3. 表格 / 分页 / 空状态统一
4. Chat SSE 与 References 精修
5. Project Detail Tabs 一致性
6. Model Gateway / GitHub / Observability 收口
7. 文案清理
8. E2E / 测试适配
9. npm typecheck / build / e2e 结果
10. 浏览器手动验证结果
11. 已知限制
12. 是否可以进入 Milestone 20C / 21
```

## 9. Claude 执行提示词

将下面内容复制给 Claude 执行：

```text
请根据项目中的文档执行 Milestone 20B。

文档路径：
docs/milestone-20b-ui-polish-and-consistency.md

执行要求：
1. 先完整阅读该文档，再检查当前 frontend 代码结构。
2. 本阶段是在 Milestone 20A 动态科技 UI 基础上做一致性收口，不要重写前端工程。
3. 不要更换技术栈，不要更换 UI 框架。
4. 不要改后端接口、数据库、认证逻辑、权限逻辑、业务流程。
5. 不要做 landing page、营销页、主题切换、国际化、复杂移动端适配。
6. 只改前端 UI、样式、展示文案、组件状态和 E2E 适配。
7. 复用现有 API client、layout、components、store、router。
8. Chat SSE 现有能力不能破坏，离开页面时 abort stream 的逻辑必须保留。
9. 登录、项目、任务、Chat、Knowledge、Repository、Members、GitHub、Model Gateway、Observability 已有功能不能回退。
10. 所有页面保持企业级 AI Coding Console 风格：深色、动态、数据密集、可读、可操作。

重点实现：
1. Dialog / Drawer 暗色统一。
2. 表格、分页、空状态、loading、error 状态统一。
3. Chat 页面输入区、消息气泡、references、SSE 状态精修。
4. Project Detail 下 Overview / Tasks / Chat / Knowledge / Repository / Members 视觉一致。
5. Model Gateway provider tags、Test Connection、config table、cost panel 收口。
6. GitHub Integration / PR Review 页面状态、cards、diff、findings 风格收口。
7. Observability audit filters、date picker、tables、metric panels 收口。
8. 清理明显中英混杂文案，主控制台 UI 尽量英文。
9. 修复 E2E 中依赖旧中文按钮的断言，优先使用 data-testid。
10. 保持 PLAYWRIGHT_BASE_URL 可覆盖。

完成后必须执行：
cd frontend
npm run typecheck
npm run build
PLAYWRIGHT_BASE_URL=http://localhost:5175 npm run test:e2e -- --workers=1

如果 5175 没启动，先运行：
cd frontend
npm run dev -- --host 0.0.0.0 --port 5175

如果后端正在运行，请手动验证：
1. 登录成功
2. Dashboard 不被 Dock 遮挡
3. Project Detail 6 个 Tab 正常
4. Repository Tab Diff 区域不被 Dock 遮挡
5. Members Joined 时间不换行
6. Task 创建和详情正常
7. Chat SSE 正常且不误报 network error
8. Chat references 展示正常
9. Knowledge 文档、chunk preview、RAG search 正常
10. Model Gateway provider cards 和 tags 无白块
11. GitHub / PR Review 页面可读
12. Observability 筛选可用
13. Logout 成功

完成后按以下格式输出：
1. 新增/修改文件清单
2. Dialog / Drawer 暗色统一
3. 表格 / 分页 / 空状态统一
4. Chat SSE 与 References 精修
5. Project Detail Tabs 一致性
6. Model Gateway / GitHub / Observability 收口
7. 文案清理
8. E2E / 测试适配
9. npm typecheck / build / e2e 结果
10. 浏览器手动验证结果
11. 已知限制
12. 是否可以进入 Milestone 20C / 21

现在开始实现，不要只给计划。
```
