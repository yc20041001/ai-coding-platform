# Milestone 20A: UI 视觉升级 Phase 2

## 1. 背景

Milestone 16A 已经完成第一轮动态科技感 UI 改造：

- 去掉传统左侧侧边栏。
- 引入 `TopCommandBar` + `FloatingDock`。
- 引入 `TechPanel`、`MetricTile`、`RuntimeBadge`、`GlowButton` 等基础视觉组件。
- 页面整体切换为深色科技风格。

当前系统功能已经通过 Milestone 19 发布验收，适合进入第二轮 UI 视觉升级。

Milestone 20A 的目标是：

> 在不改业务逻辑、不改后端接口、不破坏现有测试的前提下，把前端从“深色后台系统”升级为“更强动态感、更强产品感、更像 AI Coding 工作台”的控制台体验。

## 2. 严格边界

执行本阶段必须遵守：

1. 只改前端 UI/交互表现，不改业务逻辑。
2. 不改后端接口。
3. 不重写前端工程。
4. 不更换 Vue、Vite、Element Plus、Pinia、Router。
5. 不做 landing page、营销页。
6. 不做暗色/亮色主题切换。
7. 不做国际化。
8. 不做复杂移动端适配，只保证常见桌面宽度可用。
9. 不破坏 Chat SSE。
10. 不破坏登录、项目、任务、知识库、模型网关、GitHub、可观测性已有功能。
11. 不新增真实模型调用。
12. 不引入重型 3D 引擎，除非只用于轻量背景且 build 不明显膨胀。
13. 不把大图片、视频、无关素材加入仓库。

## 3. 视觉方向

关键词：

- AI Coding Console
- Dynamic Workspace
- Command Center
- Holographic Panels
- Flowing Navigation
- Data Dense, Not Marketing

应避免：

- 传统左侧大侧边栏。
- 大面积普通白卡片。
- 营销式 hero page。
- 只靠渐变色堆叠。
- 页面内容过度稀疏。
- 文字漂浮遮挡交互区域。

## 4. 总体设计目标

### 4.1 Shell 体验

现有结构：

```text
BasicLayout
  TopCommandBar
  router-view
  FloatingDock
```

本阶段继续沿用，但增强：

1. `TopCommandBar`
   - 增加更强的命令中心感。
   - 显示当前模块、运行环境、模型 Provider、用户身份。
   - 保持 logout 和用户信息可访问。
   - 不做复杂全局搜索，除非以静态 command input 占位。

2. `FloatingDock`
   - 改造成更动态的导航系统。
   - active item 需要有明显流光/能量条/聚焦态。
   - hover 时有轻微升起、辉光、label 展开或背景流动。
   - 非 ADMIN 仍隐藏 Observability 和 Model Gateway。
   - 图标建议使用已有符号或引入轻量图标，但不要造成大包体积。

3. `App Background`
   - 保持深色科技感。
   - 增加轻量 animated mesh/grid/scanline。
   - 背景必须不干扰文字可读性。
   - 动画要低成本，可通过 CSS 实现。

### 4.2 页面体验

优先升级以下页面：

1. Dashboard
2. Project List
3. Project Detail
4. Task List
5. Task Detail
6. Chat
7. Knowledge
8. Model Gateway
9. GitHub Integration / PR Review
10. Observability

每个页面应做到：

- 有明确页面标题和上下文信息。
- 有工作台式主区域，而不是简单卡片堆叠。
- 数据密度足够，适合企业控制台。
- Loading/Empty/Error 状态与科技风格统一。
- 表格、弹窗、抽屉仍可读、可操作。

## 5. 建议新增/修改组件

### 5.1 建议新增组件

```text
frontend/src/shared/components/DynamicWorkspace.vue
frontend/src/shared/components/CommandSurface.vue
frontend/src/shared/components/SignalStrip.vue
frontend/src/shared/components/NeonDivider.vue
frontend/src/shared/components/StatusPulse.vue
frontend/src/shared/components/DataOrbit.vue
frontend/src/shared/components/SectionRail.vue
```

允许根据实际需要减少，但建议至少新增：

- `DynamicWorkspace.vue`
- `SignalStrip.vue`
- `StatusPulse.vue`

### 5.2 组件说明

#### DynamicWorkspace.vue

用途：

- 页面级布局容器。
- 提供统一背景层、顶部信息区、内容 slot。

Props 建议：

```ts
interface Props {
  title: string
  subtitle?: string
  eyebrow?: string
  status?: string
}
```

Slots：

```text
actions
metrics
default
```

#### SignalStrip.vue

用途：

- 小型流动状态条。
- 用于页面顶部、Dock active 状态、Panel header。

Props 建议：

```ts
interface Props {
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'muted'
  active?: boolean
}
```

#### StatusPulse.vue

用途：

- 替代普通状态圆点。
- 展示 UP、RUNNING、COMPLETED、FAILED、MOCK、CONNECTED 等状态。

Props 建议：

```ts
interface Props {
  status: string
  tone?: 'success' | 'warning' | 'danger' | 'primary' | 'muted'
}
```

## 6. 页面改造要求

### 6.1 Dashboard

目标：

将 Dashboard 改成“系统运行态总览”，而不是普通指标卡页面。

需要实现：

- 顶部动态工作台 header。
- 核心指标使用 `MetricTile` 或增强版 tile。
- 增加“System Flow”区域，展示：
  - Projects
  - Tasks
  - Chat
  - RAG
  - Model Calls
  - GitHub Review
- 可以使用 CSS line/rail/pulse 表现数据流，但不要引入重型图表库。

不得做：

- 不新增后端接口。
- 不使用假数据覆盖真实接口返回。

### 6.2 Project List

目标：

把项目列表变成“项目运行空间入口”。

需要实现：

- 保留表格或列表的可扫描性。
- 项目行 hover 有更明显聚焦态。
- 创建项目按钮更突出，但不喧宾夺主。
- 项目数量、ACTIVE 状态、最近创建时间用更清楚的视觉层级呈现。

### 6.3 Project Detail

目标：

项目详情要像“单个项目的指挥台”。

需要实现：

- 顶部项目 header 更强。
- Tabs 从普通 tab 升级为动态 segment rail。
- Overview/Tasks/Chat/Knowledge/Repository/Members 保持路由逻辑不变。
- 当前项目角色、状态、技术栈有更清晰展示。

### 6.4 Task List / Task Detail

目标：

强化 Agent 执行流、状态机和产物感。

需要实现：

- Task 状态用 `StatusPulse` 或类似组件展示。
- Task Detail 增加执行过程视觉区：
  - Pending
  - Running
  - Model Gateway
  - Artifact
  - Completed
- Logs/Artifacts/Executions/Model Logs 的抽屉或 Tab 保持可读。

不得做：

- 不改 Task 状态机。
- 不改 execute 接口。

### 6.5 Chat

目标：

Chat 页面要像 AI 工作流对话台，不像普通聊天页面。

需要实现：

- 左侧会话列表不做传统侧栏厚重样式，可做轻量 session rail。
- 消息区域增加 streaming 状态视觉。
- RAG references 显示更精致：
  - score
  - source
  - snippet
  - filePath
- SSE token 流必须保持正常。
- 页面离开时 abort stream 的逻辑不能破坏。

### 6.6 Knowledge

目标：

知识库页面要像“项目记忆系统”。

需要实现：

- Knowledge Base 列表视觉增强。
- Document chunks preview 更清晰。
- RAG search result 增强 score/snippet/source 展示。
- 空状态更像系统提示，不像普通空盒子。

### 6.7 Model Gateway

目标：

模型网关页面要体现 Provider、Fallback、Safety、Cost 的运行状态。

需要实现：

- Provider 卡片更有层次。
- MOCK/Claude/OpenAI Compatible 状态更清晰。
- Usage Cost Panel 更像仪表盘。
- Connection Test Dialog 保持可用。

### 6.8 GitHub Integration / PR Review

目标：

GitHub 页面要像“代码审查作战台”。

需要实现：

- OAuth 未配置时显示专业空状态。
- Repository/PR 列表视觉增强。
- PR Review finding 风险等级更突出。
- Patch/Diff 展示保持可读。

### 6.9 Observability

目标：

Observability 页面要像“系统遥测中心”。

需要实现：

- Overview 指标更像 telemetry。
- Model usage 显示 token/cost/request trend。
- Audit logs 筛选保持清晰。
- 表格不要变成低可读性的炫酷样式。

## 7. CSS 与主题要求

### 7.1 Token 优先

继续使用 `frontend/src/styles/index.css` 中的 CSS variables：

```css
--app-bg
--app-panel
--app-panel-strong
--app-border
--app-border-strong
--app-text
--app-text-muted
--app-primary
--app-accent
--app-success
--app-warning
--app-danger
```

如需新增 token，必须集中放在 `index.css`。

### 7.2 Element Plus 覆盖

如需增强表格、弹窗、抽屉、输入框：

- 优先修改 `frontend/src/styles/element.css`。
- 不要在每个页面写大量重复 Element Plus override。
- 保持表单可读性和焦点状态。

### 7.3 动画限制

允许：

- CSS transition
- CSS keyframes
- transform/opacity/filter 轻量动画

不建议：

- 长时间高频大面积 blur 动画。
- 大量 box-shadow 动画。
- 重型 canvas/three.js 背景。
- 自动播放视频背景。

必须考虑：

```css
@media (prefers-reduced-motion: reduce) {
  /* 降低或关闭动画 */
}
```

## 8. 验收标准

### 8.1 自动化检查

必须通过：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

建议额外执行：

```bash
cd backend
mvn test
```

### 8.2 浏览器手动验收

必须检查：

1. Login
2. Dashboard
3. Projects
4. Project Detail Tabs
5. Members
6. Repository
7. Tasks
8. Task Detail
9. Chat SSE
10. Chat references
11. Knowledge + RAG Search
12. Agents
13. Model Gateway
14. GitHub Integration / PR Review
15. Observability
16. Logout

### 8.3 视觉验收

必须满足：

- 页面不再呈现传统左侧后台布局。
- 导航具有动态 dock/command center 感。
- 页面科技感增强，但核心数据仍清晰可读。
- 表格、表单、弹窗、抽屉可正常使用。
- 长文本不溢出按钮/卡片/表格。
- 1366x768 和 1440x900 下主要页面不明显遮挡。
- Chat SSE 流式输出正常。
- References、logs、artifacts、model logs 可读。

## 9. 禁止事项

本阶段禁止：

- 删除现有页面。
- 删除现有测试。
- 改后端接口。
- 改认证逻辑。
- 改 API client 行为。
- 用假数据替换真实 API。
- 引入 Tailwind、Naive UI、Ant Design Vue 等新 UI 栈。
- 引入大量图片素材。
- 把视觉升级写成 landing page。

## 10. 完成报告格式

完成后输出：

```text
Milestone 20A 完成报告

1. 新增/修改文件清单
2. Shell / Navigation 改造说明
3. Dashboard 改造说明
4. Project / Task / Chat / Knowledge 改造说明
5. Model Gateway / GitHub / Observability 改造说明
6. 统一组件与 CSS token 说明
7. typecheck / build / E2E 结果
8. 浏览器手动验收结果
9. 已知限制
10. 是否可以进入 Milestone 20B：真实部署
```

## 11. Claude 执行提示词

可以直接把下面这段发给 Claude：

```text
请根据项目中的文档执行 Milestone 20A。

文档路径：
docs/milestone-20a-ui-visual-upgrade-phase-2.md

执行要求：
1. 先完整阅读该文档，再检查当前 frontend 代码结构。
2. 本阶段只做前端 UI 视觉升级，不改业务逻辑。
3. 不改后端接口，不改 API client 行为，不改认证逻辑。
4. 不重写前端工程，不更换 Vue/Vite/Element Plus/Pinia/Router。
5. 不恢复传统左侧侧边栏。
6. 不做 landing page、营销页、暗色/亮色主题切换、国际化。
7. 复用现有组件：TopCommandBar、FloatingDock、TechPanel、MetricTile、RuntimeBadge、GlowButton、LoadingState、ErrorState、ConfirmButton。
8. 可以新增少量共享视觉组件，例如 DynamicWorkspace、SignalStrip、StatusPulse。
9. 优先升级 Dashboard、Project List、Project Detail、Task List、Task Detail、Chat、Knowledge、Model Gateway、GitHub、Observability。
10. Chat SSE 现有能力不能破坏，离开页面 abort stream 的逻辑不能破坏。
11. 所有页面必须保持企业控制台可读性，表格、表单、弹窗、抽屉仍然清晰可用。
12. 动画使用轻量 CSS，不引入重型 3D/canvas/video 依赖。
13. 如需新增 CSS token，集中放在 frontend/src/styles/index.css。
14. 如需覆盖 Element Plus，集中放在 frontend/src/styles/element.css。
15. 不要提交 frontend/dist、node_modules、.env。

必须执行：
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1

如果后端和前端正在运行，请手动验证：
1. Login
2. Dashboard
3. Projects
4. Project Detail Tabs
5. Members
6. Repository
7. Tasks
8. Task Detail
9. Chat SSE
10. Chat references
11. Knowledge + RAG Search
12. Agents
13. Model Gateway
14. GitHub Integration / PR Review
15. Observability
16. Logout

完成后按以下格式输出：
1. 新增/修改文件清单
2. Shell / Navigation 改造说明
3. Dashboard 改造说明
4. Project / Task / Chat / Knowledge 改造说明
5. Model Gateway / GitHub / Observability 改造说明
6. 统一组件与 CSS token 说明
7. typecheck / build / E2E 结果
8. 浏览器手动验收结果
9. 已知限制
10. 是否可以进入 Milestone 20B：真实部署

现在开始实现，不要只给计划。
```

