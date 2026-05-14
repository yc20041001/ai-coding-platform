# Milestone 12: 前端 Vue 3 企业级控制台基础工程实施文档

## 1. 背景与目标

当前后端已经完成 AI Coding Platform 的 P0 主链路，并具备统一 smoke test、审计日志、模型用量统计和本地 Docker Compose 环境。

前端目前只有目录骨架：

```text
frontend/
  public/
  src/
    app/
    modules/
    shared/
    styles/
```

Milestone 12 的目标是：

> 初始化 Vue 3 + Vite + TypeScript 企业级控制台基础工程，完成登录、主布局、项目、任务、Chat、Agent、Knowledge Base、Observability 等首批页面骨架，并接入后端 API 主链路。

本阶段重点是“可用的企业后台控制台”，不是营销页，也不是炫技型视觉页面。

## 2. 实施边界

### 2.1 本阶段要做

- 初始化 Vite + Vue 3 + TypeScript 工程
- 接入 Element Plus
- 接入 Pinia
- 接入 Vue Router
- 接入 Axios API Client
- 登录页
- Token 管理
- 路由守卫
- 主布局
- 项目列表页
- 项目详情页
- Task 页面
- Chat 页面
- Agent 页面
- Knowledge Base 页面
- Observability 页面
- 基础错误处理
- `.env.example`
- 前端 README 或主 README 更新

### 2.2 本阶段不做

- 不做复杂设计系统
- 不做移动端深度适配
- 不做 Monaco Editor 深度集成
- 不做真实代码编辑器
- 不做复杂权限菜单系统
- 不做 WebSocket
- 不做真实文件上传组件高级能力
- 不做 E2E 自动化测试
- 不做前端国际化
- 不做暗色主题

## 3. 设计原则

- 企业级控制台风格：信息密度适中，安静、清晰、可扫描
- 不做 landing page
- 首屏就是登录或控制台
- 页面以业务操作为核心
- 组件简洁，不堆装饰性卡片
- 表格、表单、详情、抽屉、弹窗优先
- 保持和后端 API 一致
- 所有 ID 对外以 String 处理
- 所有 API 响应统一处理 `ApiResponse`
- 不在 localStorage 存储敏感业务数据，只存 token 和必要用户信息

## 4. 技术栈

| 分类 | 技术 |
|---|---|
| Framework | Vue 3 |
| Build | Vite |
| Language | TypeScript |
| Router | Vue Router |
| State | Pinia |
| UI | Element Plus |
| HTTP | Axios |
| Icons | @element-plus/icons-vue |
| Markdown | markdown-it |
| Code highlight | highlight.js |

## 5. 目录结构

沿用当前目录骨架，补齐文件：

```text
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── .env.example
├── README.md
└── src/
    ├── main.ts
    ├── App.vue
    ├── styles/
    │   ├── index.css
    │   └── element.css
    ├── app/
    │   ├── router/
    │   │   └── index.ts
    │   ├── guards/
    │   │   └── authGuard.ts
    │   ├── store/
    │   │   └── index.ts
    │   └── layouts/
    │       ├── BasicLayout.vue
    │       └── AuthLayout.vue
    ├── shared/
    │   ├── api/
    │   │   ├── client.ts
    │   │   ├── types.ts
    │   │   └── endpoints.ts
    │   ├── components/
    │   │   ├── PageHeader.vue
    │   │   ├── EmptyState.vue
    │   │   ├── StatusTag.vue
    │   │   └── MarkdownRenderer.vue
    │   ├── composables/
    │   │   ├── useRequest.ts
    │   │   └── usePagination.ts
    │   ├── constants/
    │   │   └── storage.ts
    │   ├── types/
    │   │   └── common.ts
    │   └── utils/
    │       ├── auth.ts
    │       ├── format.ts
    │       └── sse.ts
    └── modules/
        ├── auth/
        │   ├── api.ts
        │   ├── store.ts
        │   └── pages/LoginPage.vue
        ├── dashboard/
        │   └── pages/DashboardPage.vue
        ├── project/
        │   ├── api.ts
        │   ├── types.ts
        │   └── pages/
        │       ├── ProjectListPage.vue
        │       └── ProjectDetailPage.vue
        ├── task/
        │   ├── api.ts
        │   ├── types.ts
        │   └── pages/TaskListPage.vue
        ├── chat/
        │   ├── api.ts
        │   ├── types.ts
        │   └── pages/ChatPage.vue
        ├── agent/
        │   ├── api.ts
        │   ├── types.ts
        │   └── pages/AgentListPage.vue
        ├── knowledge/
        │   ├── api.ts
        │   ├── types.ts
        │   └── pages/KnowledgeBasePage.vue
        └── admin/
            ├── api.ts
            └── pages/ObservabilityPage.vue
```

## 6. 工程初始化

### 6.1 package.json

新增：

```json
{
  "name": "ai-coding-platform-frontend",
  "version": "0.0.1",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview --host 0.0.0.0",
    "typecheck": "vue-tsc --noEmit"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.1",
    "axios": "^1.7.9",
    "element-plus": "^2.9.1",
    "highlight.js": "^11.11.1",
    "markdown-it": "^14.1.0",
    "pinia": "^2.3.0",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "typescript": "^5.7.2",
    "vite": "^6.0.5",
    "vue-tsc": "^2.2.0"
  }
}
```

版本可按当前最新兼容版本微调。

### 6.2 环境变量

新增：

```text
frontend/.env.example
```

内容：

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=AI Coding Platform
```

### 6.3 Vite 配置

`vite.config.ts`：

- Vue plugin
- `@` alias 指向 `src`
- dev server 默认 `5173`

## 7. API Client 设计

### 7.1 ApiResponse 类型

```ts
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
  timestamp?: string
}
```

### 7.2 PageResult 类型

```ts
export interface PageResult<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
  hasNext: boolean
}
```

### 7.3 Axios Client

`shared/api/client.ts` 要求：

- baseURL = `import.meta.env.VITE_API_BASE_URL`
- 请求拦截器自动加 `Authorization: Bearer <token>`
- 响应拦截器处理 `ApiResponse`
- `code !== OK` 时抛出业务错误
- `UNAUTHORIZED` 时清理 token 并跳转登录
- 保留 `traceId` 便于排查

## 8. Auth 模块

### 8.1 登录页

路径：

```text
/login
```

页面：

```text
modules/auth/pages/LoginPage.vue
```

功能：

- email 输入
- password 输入
- 登录按钮
- loading 状态
- 错误提示
- 默认可填充开发账号：
  - admin@example.com
  - Admin@123456

### 8.2 Auth Store

```text
modules/auth/store.ts
```

状态：

- token
- refreshToken
- user
- roles
- permissions

方法：

- login
- logout
- fetchMe
- restoreFromStorage

### 8.3 路由守卫

规则：

- 未登录访问业务页跳转 `/login`
- 已登录访问 `/login` 跳转 `/`
- 401 自动 logout

## 9. Layout 设计

### 9.1 BasicLayout

结构：

```text
左侧菜单 + 顶部栏 + 内容区
```

菜单项：

- Dashboard
- Projects
- Agents
- Knowledge Base
- Observability

项目详情内二级 Tab：

- Overview
- Tasks
- Chat
- Knowledge
- Repository
- Members

### 9.2 顶部栏

包含：

- 当前应用名
- 当前用户
- Logout

## 10. 路由设计

```ts
const routes = [
  { path: '/login', component: LoginPage },
  {
    path: '/',
    component: BasicLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: DashboardPage },
      { path: 'projects', component: ProjectListPage },
      { path: 'projects/:projectId', component: ProjectDetailPage },
      { path: 'projects/:projectId/tasks', component: TaskListPage },
      { path: 'projects/:projectId/chat', component: ChatPage },
      { path: 'projects/:projectId/knowledge', component: KnowledgeBasePage },
      { path: 'agents', component: AgentListPage },
      { path: 'observability', component: ObservabilityPage }
    ]
  }
]
```

## 11. 页面设计

## 11.1 DashboardPage

功能：

- 调用 `/api/observability/overview`
- 展示：
  - projectCount
  - taskCount
  - agentCount
  - knowledgeBaseCount
  - modelRequestCount
  - todayTokenUsage

UI：

- 顶部 PageHeader
- 指标卡片
- 最近项目入口

## 11.2 ProjectListPage

接口：

- `GET /api/projects`
- `POST /api/projects`

功能：

- 项目列表
- 创建项目弹窗
- 点击进入项目详情

字段：

- name
- description
- techStack
- status
- createTime

## 11.3 ProjectDetailPage

接口：

- `GET /api/projects/{projectId}`
- `GET /api/projects/{projectId}/overview`

功能：

- 项目基础信息
- overview 指标
- Tabs 导航到 Tasks / Chat / Knowledge / Repository / Members

## 11.4 TaskListPage

接口：

- `GET /api/projects/{projectId}/tasks`
- `POST /api/projects/{projectId}/tasks`
- `POST /api/tasks/{taskId}/execute`
- `GET /api/tasks/{taskId}/logs`
- `GET /api/tasks/{taskId}/artifacts`

功能：

- 任务列表
- 创建任务
- 执行任务
- 查看状态
- 查看日志
- 查看产物

UI：

- 表格
- 创建任务 Drawer
- 日志 Drawer
- 产物 Drawer

## 11.5 ChatPage

接口：

- `POST /api/projects/{projectId}/chat/sessions`
- `GET /api/projects/{projectId}/chat/sessions`
- `GET /api/chat/sessions/{sessionId}/messages`
- `POST /api/chat/sessions/{sessionId}/messages`
- `GET /api/chat/sessions/{sessionId}/stream`

功能：

- 会话列表
- 创建会话
- 消息列表
- 输入消息
- SSE 流式显示
- references 展示

注意：

- SSE 使用 `fetch` 读取 stream 或 EventSource polyfill 思路
- 因为需要 Authorization header，推荐使用 `fetch` stream
- token event 追加内容
- done event 更新状态和 references

## 11.6 AgentListPage

接口：

- `GET /api/agents`
- `GET /api/agents/{agentId}`

功能：

- Agent 列表
- 类型筛选
- 状态筛选
- 查看详情

## 11.7 KnowledgeBasePage

接口：

- `GET /api/projects/{projectId}/knowledge-bases`
- `POST /api/projects/{projectId}/knowledge-bases`
- `POST /api/projects/{projectId}/knowledge-documents`
- `GET /api/knowledge-bases/{knowledgeBaseId}/documents`
- `GET /api/knowledge-documents/{documentId}/chunks`
- `POST /api/projects/{projectId}/rag/search`

功能：

- 知识库列表
- 创建知识库
- 上传 Markdown/Text/Code 文档
- 文档列表
- chunk 预览
- RAG 搜索测试

## 11.8 ObservabilityPage

接口：

- `GET /api/observability/overview`
- `GET /api/observability/model-usage/summary`
- `GET /api/audit/logs`

功能：

- 系统概览
- 模型用量
- 审计日志表格

## 12. Shared Components

## 12.1 PageHeader

Props：

- title
- description
- actions slot

## 12.2 EmptyState

用于空列表。

## 12.3 StatusTag

根据状态返回 Element Plus tag 类型：

- ACTIVE / COMPLETED / SUCCESS → success
- RUNNING / PROCESSING / STREAMING → warning
- FAILED / DISABLED / CANCELED → danger/info

## 12.4 MarkdownRenderer

使用：

- markdown-it
- highlight.js

用于：

- Chat assistant message
- Task artifacts
- RAG chunk preview

## 13. 样式规范

`styles/index.css`：

- 全局 reset
- body 背景 `#f5f7fb`
- 内容区最大宽度不强制限制
- 控制台页面使用紧凑布局
- 卡片圆角不超过 8px
- 表格页面避免大面积营销式 hero

色彩：

- 主色使用 Element Plus 默认蓝
- 背景浅灰
- 文本深灰
- 状态色使用 Element Plus 语义色

## 14. 错误处理

统一处理：

- 网络错误
- `UNAUTHORIZED`
- `FORBIDDEN`
- `PROJECT_ACCESS_DENIED`
- `VALIDATION_ERROR`
- `INTERNAL_ERROR`

UI：

- 使用 `ElMessage.error`
- 表单错误显示在字段下方
- 请求 loading 防重复提交

## 15. 验收标准

### 15.1 安装

```bash
cd frontend
npm install
```

### 15.2 类型检查

```bash
npm run typecheck
```

### 15.3 构建

```bash
npm run build
```

### 15.4 启动

```bash
npm run dev
```

访问：

```text
http://localhost:5173
```

### 15.5 手动验证

必须验证：

1. 登录成功
2. 刷新页面 token 保持
3. 退出登录成功
4. 项目列表加载
5. 创建项目成功
6. 进入项目详情
7. 创建任务成功
8. 执行任务成功
9. 查看任务日志/产物
10. 创建知识库成功
11. 上传文档成功
12. RAG 搜索成功
13. 创建 Chat 会话
14. 发送 Chat 消息
15. SSE 流式输出显示
16. Chat references 展示
17. Agent 列表加载
18. Observability 页面加载

## 16. 后端依赖

本阶段依赖后端已启动：

```bash
cd backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080
```

前端环境变量：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## 17. 完成报告模板

完成后请按以下格式输出：

```markdown
# Milestone 12 完成报告

## 1. 新增/修改文件清单

...

## 2. 前端工程初始化说明

...

## 3. 路由与布局说明

...

## 4. API Client 与 Auth 说明

...

## 5. 页面清单与功能说明

...

## 6. Chat SSE 实现说明

...

## 7. npm install / typecheck / build 结果

...

## 8. 手动验证结果

...

## 9. 已知限制

...

## 10. 是否可以进入 Milestone 13：前端功能完善与体验优化

...
```

## 18. Milestone 13 预告

如果 Milestone 12 验证通过，下一阶段建议进入：

```text
Milestone 13: 前端功能完善与体验优化
```

建议范围：

- 任务详情页
- Agent 执行详情
- Model logs 展示
- Audit logs 筛选增强
- Repository 页面
- Member 页面
- 更完整权限菜单
- 前端错误边界
- 前端 smoke test
- Playwright 基础 E2E

