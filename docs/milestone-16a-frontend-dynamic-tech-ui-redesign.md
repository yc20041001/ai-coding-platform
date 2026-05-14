# Milestone 16A: 前端动态科技感 UI 重设计

## 1. 目标

对当前 AI Coding Platform 前端进行 UI/UX 视觉升级。

当前问题：

- 左侧 sidebar 过于传统后台系统
- 页面视觉偏普通管理台
- 缺少 AI Coding 平台应有的科技感、动态感、产品辨识度
- Chat / Agent / Task 等核心 AI 场景没有突出

本阶段目标：

> 保留现有 Vue 3 + Element Plus + Pinia + Router + API 逻辑，只重做前端视觉层与布局外壳，打造更有科技感、更动态、更像 AI Coding Console 的产品界面。

## 2. 核心要求

### 2.1 必须遵守

1. 不改后端接口。
2. 不改业务逻辑。
3. 不改 Pinia store 逻辑。
4. 不改 API client 逻辑。
5. 不破坏路由结构。
6. 不破坏 Auth 登录逻辑。
7. 不破坏 Chat SSE。
8. 不破坏 Task 执行、RAG、Observability 已有功能。
9. 不更换 Vue / Vite / Element Plus 技术栈。
10. 不引入重型 UI 模板。
11. 不做 landing page。
12. 不做营销页。
13. 不做复杂暗黑大屏。
14. 不做移动端深度适配。

### 2.2 可以改

1. BasicLayout。
2. LoginPage 视觉。
3. Dashboard 视觉。
4. ProjectList / ProjectDetail 视觉。
5. Task 页面视觉。
6. Chat 页面视觉。
7. Agent 页面视觉。
8. Knowledge 页面视觉。
9. Observability 页面视觉。
10. 全局 CSS、主题变量、Element Plus 样式覆盖。
11. 新增纯 UI 组件。
12. 新增动效、背景、渐变、玻璃质感、光效。

## 3. 视觉方向

目标风格：

```text
AI Coding Console
动态科技感
半暗色控制台
玻璃质感面板
高密度信息布局
蓝 / 青 / 紫 点缀
精致但不花哨
类似 Cursor + Linear + Vercel Dashboard + Claude Console
```

不要做成：

```text
传统后台管理系统
左侧固定菜单
大屏数据可视化
营销官网
过度霓虹
过度动效
低可读性暗黑界面
```

## 4. 布局改造方向

### 4.1 当前布局

```text
左侧 sidebar + 顶部 header + 内容区
```

### 4.2 新布局目标

取消传统左侧 sidebar，改成：

```text
顶部动态导航栏
+
中央内容工作区
+
底部悬浮 Dock 导航
+
右上角用户/状态区
```

推荐结构：

```text
AppShell
├── TopCommandBar
│   ├── Brand
│   ├── Global Search / Command Entry
│   ├── Runtime Status
│   └── User Actions
├── MainWorkspace
│   └── router-view
└── FloatingDock
    ├── Dashboard
    ├── Projects
    ├── Agents
    ├── Observability (ADMIN only)
    └── Logout / User
```

## 5. BasicLayout 改造要求

文件：

```text
frontend/src/app/layouts/BasicLayout.vue
```

### 5.1 要做

1. 删除传统 `el-aside` 左侧栏。
2. 使用全屏 App Shell。
3. 顶部做成玻璃质感导航条。
4. 底部增加悬浮 Dock 导航。
5. 当前路由高亮。
6. ADMIN 才显示 Observability。
7. 保留 logout 按钮和 `data-testid="btn-logout"`。
8. 保留 `router-view`。
9. 保留原有 route/path 跳转逻辑。

### 5.2 建议 UI

顶部：

```text
AI Coding Platform    Command / Search    Online · Mock Gateway    admin
```

底部 Dock：

```text
Dashboard | Projects | Agents | Observability
```

Dock 视觉：

- 居中悬浮
- 背景 blur
- 圆角
- active 有发光边框
- hover 有轻微上浮
- 图标 + 文本
- 不遮挡内容

## 6. 全局视觉系统

文件：

```text
frontend/src/styles/index.css
frontend/src/styles/element.css
```

### 6.1 建议主题

```css
:root {
  --app-bg: #070b18;
  --app-panel: rgba(15, 23, 42, 0.72);
  --app-panel-strong: rgba(15, 23, 42, 0.92);
  --app-border: rgba(148, 163, 184, 0.18);
  --app-text: #e5edf8;
  --app-text-muted: #93a4b8;
  --app-primary: #38bdf8;
  --app-accent: #8b5cf6;
  --app-success: #22c55e;
  --app-warning: #f59e0b;
  --app-danger: #ef4444;
}
```

### 6.2 背景效果

可以使用：

- `radial-gradient`
- `linear-gradient`
- subtle grid
- noise texture via CSS
- moving glow layer
- aurora 光带

不要使用：

- 大面积纯紫渐变
- 花哨 orb 堆叠
- 影响阅读的强动画
- 复杂 canvas 背景

## 7. 页面改造清单

### 7.1 LoginPage

文件：

```text
frontend/src/modules/auth/pages/LoginPage.vue
```

目标：

- 科技感登录页
- 左侧动态品牌区域
- 右侧玻璃登录卡片
- 保留原表单逻辑
- 保留测试账号
- 登录错误提示保持可见

建议布局：

```text
左：品牌 / AI Agent 状态 / 动态线条背景
右：登录卡片
```

### 7.2 DashboardPage

文件：

```text
frontend/src/modules/dashboard/pages/DashboardPage.vue
```

目标：

- 从普通指标卡变成 AI 工作台
- 指标卡科技化
- 增加系统运行状态视觉
- 不改 API

建议模块：

```text
System Overview
Agent Runtime
Task Pipeline
Model Usage
Recent Activity
```

### 7.3 ProjectListPage

目标：

- 项目列表保留表格
- 顶部改成项目工作台 Header
- 创建项目按钮更突出
- 表格容器科技面板化

### 7.4 ProjectDetailPage

目标：

- 项目详情顶部改成 Project Command Header
- Tabs 改为顶部 segmented controls 或横向胶囊导航
- 不使用左栏
- Repository / Members / Tasks / Chat / Knowledge 保持可用

### 7.5 TaskListPage / TaskDetailPage

目标：

- Task 状态更明显
- 执行按钮更像 AI Run
- Logs / Artifacts / Executions 用科技面板展示
- 状态颜色统一

### 7.6 ChatPage

这是最重要的页面。

目标：

```text
AI Coding Workspace
```

建议布局：

```text
左：Session Rail
中：Chat Stream
右：Context / References / Agent
```

如果不想加三栏复杂布局，可以做：

```text
上：Session Header
中：Message Stream
下：Prompt Composer
右侧 Drawer 展示 References
```

要求：

- SSE 逻辑不改
- sendMessage 逻辑不改
- references 展示更精致
- streaming 状态有光标/流式效果
- 消息卡片更像 AI 产品

### 7.7 AgentListPage

目标：

- 从表格改成卡片网格或表格 + 卡片混合
- Agent 类型用视觉图标区分
- status、model、能力简介突出
- 不改 API

### 7.8 KnowledgeBasePage

目标：

- 知识库左侧不做传统栏，可做 panel grid
- 文档列表、chunk preview、RAG search 更有科技感
- 搜索结果展示 score、filePath、snippet

### 7.9 ObservabilityPage

目标：

- 保持 ADMIN 页面
- 指标卡、模型用量、审计日志科技化
- 筛选器更紧凑
- 表格可读性优先

## 8. 新增 UI 组件建议

可以新增：

```text
frontend/src/shared/components/AppShell.vue
frontend/src/shared/components/TopCommandBar.vue
frontend/src/shared/components/FloatingDock.vue
frontend/src/shared/components/TechPanel.vue
frontend/src/shared/components/MetricTile.vue
frontend/src/shared/components/GlowButton.vue
frontend/src/shared/components/RuntimeBadge.vue
frontend/src/shared/components/CommandSearch.vue
```

但注意：

- 新组件只负责 UI
- 不承载业务逻辑
- 不重复 API 调用
- 不破坏现有组件

## 9. 动效规范

允许：

- hover 上浮 2-4px
- active glow
- background gradient 缓慢移动
- loading shimmer
- streaming cursor blink
- panel fade in

不允许：

- 大量旋转
- 页面跳动
- 频繁闪烁
- 影响表格阅读
- 导航位置不稳定

## 10. 验证要求

完成后必须执行：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e
```

如果后端正在运行，手动验证：

1. 登录成功。
2. Dashboard 可打开。
3. 底部 Dock 导航正常。
4. 非 ADMIN 不显示 Observability。
5. Project 列表正常。
6. Project Detail Tabs 正常。
7. Task 创建和执行正常。
8. Chat SSE 正常。
9. Chat references 正常。
10. Knowledge 上传和 RAG Search 正常。
11. Observability 正常。
12. Logout 成功。

## 11. 验收标准

必须满足：

- 没有传统左侧 sidebar。
- 页面整体明显更有科技感。
- 导航更动态。
- 不影响任何原有业务功能。
- 不破坏 E2E。
- 不破坏 Chat SSE。
- 不改后端接口。
- 不出现页面文字重叠。
- 表格和表单仍然可读。
- 企业工具感强于装饰感。

## 12. Claude 执行提示词

可以直接发送以下内容给 Claude：

```text
请根据项目中的文档执行 Milestone 16A：前端动态科技感 UI 重设计。

文档路径：
docs/milestone-16a-frontend-dynamic-tech-ui-redesign.md

执行要求：
1. 先完整阅读该文档，再检查当前 frontend 代码结构。
2. 本阶段只做 UI/UX 视觉升级，不改业务逻辑。
3. 保留 Vue 3 + Vite + Element Plus + Pinia + Router。
4. 不改后端接口。
5. 不改 API client 的请求逻辑。
6. 不破坏 Auth、Project、Task、Chat、Knowledge、Observability 已有功能。
7. 不破坏 Chat SSE。
8. 不做 landing page。
9. 不做营销页。
10. 不做传统左侧 sidebar。
11. 把现有 BasicLayout 改造成动态科技感 App Shell。
12. 使用顶部 Command Bar + 底部 Floating Dock + 中央工作区。
13. ADMIN 才显示 Observability。
14. 保留 logout 功能和 data-testid="btn-logout"。
15. 所有页面保持企业控制台可读性，不要为了视觉牺牲表格和表单。
16. 允许新增纯 UI 组件，但不要把业务逻辑搬进去。
17. 所有改动尽量集中在 layout、styles、页面视觉和共享 UI 组件。

需要实现：
1. 重做 BasicLayout，移除左侧 sidebar。
2. 新增或实现 TopCommandBar。
3. 新增或实现 FloatingDock。
4. 新增 TechPanel / MetricTile / RuntimeBadge 等纯 UI 组件。
5. 重做全局背景和主题变量。
6. 优化 LoginPage 科技感。
7. 优化 DashboardPage。
8. 优化 ProjectListPage / ProjectDetailPage。
9. 优化 TaskListPage / TaskDetailPage。
10. 优化 ChatPage，突出 AI Coding Workspace 质感。
11. 优化 AgentListPage。
12. 优化 KnowledgeBasePage。
13. 优化 ObservabilityPage。
14. 保证移动端至少不崩，但不做深度移动端适配。

完成后必须执行：
cd frontend
npm run typecheck
npm run build
npm run test:e2e

如果后端正在运行，请手动验证：
1. 登录成功。
2. Dock 导航正常。
3. Dashboard 正常。
4. Project Detail Tabs 正常。
5. Task 创建/执行正常。
6. Chat SSE 正常。
7. Knowledge RAG 正常。
8. Observability 正常。
9. Logout 成功。

完成后按以下格式输出：
1. 新增/修改文件清单
2. Layout 改造说明
3. 动态导航实现说明
4. 全局视觉系统说明
5. 各页面 UI 优化说明
6. Chat 页面优化说明
7. typecheck / build / e2e 结果
8. 手动验证结果
9. 已知限制
10. 是否可以继续进入 Milestone 16：真实模型网关接入

现在开始实现，不要只给计划。
```
