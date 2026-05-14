# Milestone 13: 前端功能完善与体验优化实施文档

## 1. 背景与目标

Milestone 12 已经完成 Vue 3 企业级控制台基础工程，覆盖：

- 登录
- Dashboard
- 项目列表
- 项目详情
- Task 管理
- Chat SSE
- Agent 列表
- Knowledge Base
- Observability

当前前端已经“能用”，但仍存在功能空缺和体验不足：

- Repository / Members Tab 仍是占位
- Task 缺少详情页
- Agent Execution / Model Logs 缺少展示
- Audit Logs 筛选较弱
- Chat references 展示较基础
- loading / empty / error 状态不够统一
- 缺少前端 smoke test / E2E 验证

Milestone 13 的目标是：

> 在不重构 Milestone 12 前端架构的前提下，补齐关键业务页面，统一交互状态，增加基础前端质量验证，让控制台从“可用”变成“顺手”。

## 2. 实施边界

### 2.1 本阶段要做

- Project Detail 增加 Repository Tab
- Project Detail 增加 Members Tab
- Task Detail 页面
- Agent Execution Detail 展示
- Model Logs 展示
- Audit Logs 筛选增强
- Chat references 展示优化
- Knowledge 文档/chunk 体验优化
- 统一 loading / empty / error 状态
- 基础权限菜单控制
- 前端 smoke test 文档
- 可选 Playwright 基础 E2E
- 构建体积基础优化

### 2.2 本阶段不做

- 不重写现有布局
- 不更换 UI 框架
- 不做复杂设计系统
- 不做暗色主题
- 不做国际化
- 不做移动端深度适配
- 不做 Monaco Editor 集成
- 不做复杂权限矩阵管理后台
- 不改后端接口，除非发现明确 bug

## 3. 设计原则

- 最小侵入式完善
- 复用现有 API client、layout、components
- 业务页面优先于视觉装饰
- 操作反馈明确
- 表格页面提供筛选、刷新、分页
- 详情页面提供返回路径
- 抽屉用于日志、产物、引用等辅助详情
- 错误状态可见且可恢复

## 4. 新增与修改文件总览

### 4.1 建议新增文件

```text
frontend/src/modules/repository/api.ts
frontend/src/modules/repository/types.ts
frontend/src/modules/repository/pages/RepositoryPanel.vue

frontend/src/modules/member/api.ts
frontend/src/modules/member/types.ts
frontend/src/modules/member/pages/MemberPanel.vue

frontend/src/modules/task/pages/TaskDetailPage.vue
frontend/src/modules/task/components/TaskLogDrawer.vue
frontend/src/modules/task/components/TaskArtifactDrawer.vue
frontend/src/modules/task/components/AgentExecutionDrawer.vue

frontend/src/modules/admin/components/AuditLogFilters.vue
frontend/src/modules/admin/components/ModelUsagePanel.vue

frontend/src/modules/chat/components/ReferenceList.vue
frontend/src/modules/knowledge/components/ChunkPreviewDrawer.vue

frontend/src/shared/components/ErrorState.vue
frontend/src/shared/components/LoadingState.vue
frontend/src/shared/components/ConfirmButton.vue

docs/frontend-smoke-test-plan.md
```

### 4.2 建议修改文件

```text
frontend/src/app/router/index.ts
frontend/src/app/layouts/BasicLayout.vue
frontend/src/modules/project/pages/ProjectDetailPage.vue
frontend/src/modules/task/pages/TaskListPage.vue
frontend/src/modules/chat/pages/ChatPage.vue
frontend/src/modules/knowledge/pages/KnowledgeBasePage.vue
frontend/src/modules/admin/pages/ObservabilityPage.vue
frontend/src/shared/api/client.ts
frontend/src/shared/components/StatusTag.vue
frontend/src/shared/components/MarkdownRenderer.vue
frontend/package.json
frontend/README.md
```

## 5. Repository Panel

## 5.1 页面位置

在项目详情页新增或补齐：

```text
/projects/:projectId/repository
```

或作为 `ProjectDetailPage` 内 Tab：

```text
Repository
```

## 5.2 API

使用后端已有接口：

| Method | Endpoint | 说明 |
|---|---|---|
| GET | `/api/github/repositories` | GitHub 仓库列表，当前可能为空 |
| POST | `/api/projects/{projectId}/repository/bind` | 绑定仓库 |
| POST | `/api/projects/{projectId}/repository/clone` | Clone |
| POST | `/api/projects/{projectId}/repository/pull` | Pull |
| GET | `/api/projects/{projectId}/repository/branches` | 分支列表 |
| GET | `/api/projects/{projectId}/repository/diff` | Diff |

## 5.3 功能

- 展示当前仓库绑定状态
- 手动输入 repoUrl 绑定仓库
- Clone 仓库
- Pull 指定分支
- 查看分支列表
- 查看 diff

## 5.4 UI

- 顶部仓库状态区
- 操作按钮：
  - Bind
  - Clone
  - Pull
  - Refresh Branches
- 分支表格
- Diff Drawer

## 5.5 验收

- 无仓库时显示 EmptyState
- 绑定仓库成功后刷新状态
- branches 能展示
- diff 查询失败时展示错误，不阻塞页面

## 6. Members Panel

## 6.1 页面位置

项目详情 Tab：

```text
Members
```

## 6.2 API

| Method | Endpoint | 说明 |
|---|---|---|
| GET | `/api/projects/{projectId}/members` | 成员列表 |
| POST | `/api/projects/{projectId}/members` | 邀请成员 |
| PUT | `/api/projects/{projectId}/members/{userId}/role` | 更新角色 |
| DELETE | `/api/projects/{projectId}/members/{userId}` | 移除成员 |

## 6.3 功能

- 成员列表
- 邀请成员
- 修改角色
- 移除成员
- 自己不能移除自己时显示后端错误

## 6.4 UI

- 表格字段：
  - username / email
  - role
  - status
  - joinTime
  - actions
- 邀请成员 Dialog
- 角色选择 Select
- 删除 Confirm

## 7. Task Detail

## 7.1 路由

新增：

```text
/tasks/:taskId
```

或者：

```text
/projects/:projectId/tasks/:taskId
```

推荐第二种，保留项目上下文。

## 7.2 API

| Method | Endpoint | 说明 |
|---|---|---|
| GET | `/api/tasks/{taskId}` | 任务详情 |
| GET | `/api/tasks/{taskId}/logs` | 日志 |
| GET | `/api/tasks/{taskId}/artifacts` | 产物 |
| GET | `/api/tasks/{taskId}/executions` | Agent 执行记录 |
| GET | `/api/agent-executions/{executionId}` | 执行详情 |
| GET | `/api/agent-executions/{executionId}/model-logs` | 模型日志 |

## 7.3 页面结构

```text
Task Header
  - title
  - status
  - priority
  - agent
  - actions

Tabs:
  - Overview
  - Logs
  - Artifacts
  - Executions
  - Model Logs
```

## 7.4 功能

- 查看任务描述
- 查看状态流转
- 执行任务
- 取消/重试任务
- 查看日志
- 查看产物 Markdown
- 查看 Agent Execution inputPrompt/outputContent
- 查看 Model Logs token usage

## 8. Agent Execution Drawer

## 8.1 展示内容

- execution id
- agentName
- status
- ragUsed
- tokenUsage
- startedAt / finishedAt
- inputPrompt
- outputContent
- references

## 8.2 UI

- Drawer 宽度 60%-70%
- inputPrompt / outputContent 使用 MarkdownRenderer 或 pre-wrap
- references 使用 ReferenceList

## 9. Model Logs 展示

## 9.1 在 Task Detail 中展示

字段：

- provider
- modelName
- requestType
- promptTokens
- completionTokens
- totalTokens
- latencyMs
- success
- errorMessage
- createTime

## 9.2 在 Observability 页面展示汇总

- summary cards
- daily table

## 10. Audit Logs 筛选增强

## 10.1 当前页面

```text
modules/admin/pages/ObservabilityPage.vue
```

## 10.2 新增筛选

- actionType
- resourceType
- resourceId
- userId
- time range

## 10.3 API

| Method | Endpoint |
|---|---|
| GET | `/api/audit/logs` |

查询参数：

- page
- pageSize
- userId
- actionType
- resourceType
- resourceId
- startTime
- endTime

## 10.4 UI

- 筛选表单
- Reset
- Search
- Table
- Pagination

## 11. Chat References 优化

## 11.1 新增 ReferenceList

```text
modules/chat/components/ReferenceList.vue
```

Props：

- references
- compact

展示：

- title
- filePath
- score
- snippet

交互：

- snippet 可展开/收起
- score 使用 tag
- 空时不显示

## 11.2 ChatPage 修改

- user / assistant 消息下方展示 references
- SSE done 后更新 references
- references 不撑破消息宽度

## 12. Knowledge 体验优化

## 12.1 ChunkPreviewDrawer

展示：

- chunkIndex
- tokenCount
- content
- metadata

## 12.2 RAG Search

优化：

- 搜索结果显示 score
- 搜索结果显示 filePath
- content/snippet 支持 MarkdownRenderer 或 pre-wrap
- 无结果显示 EmptyState

## 13. 统一状态组件

## 13.1 LoadingState

用于页面级 loading。

Props：

- text

## 13.2 ErrorState

用于页面级错误。

Props：

- title
- message
- retryText

Events：

- retry

## 13.3 ConfirmButton

封装危险操作确认。

Props：

- message
- type

## 14. API Client 优化

## 14.1 统一错误对象

`shared/api/client.ts` 中将错误标准化：

```ts
export interface ApiError {
  code: string
  message: string
  traceId?: string
  status?: number
}
```

## 14.2 请求取消

本阶段可不做完整 AbortController 管理。

但 Chat SSE 页面离开时必须中止当前 stream。

## 15. 权限菜单控制

基于当前 user.roles：

- ADMIN 可看 Observability
- 非 ADMIN 隐藏 Observability
- Project 内权限当前可先依赖后端，前端只做基础显示控制

不要在前端做强安全判断，后端仍是最终权限源。

## 16. 构建体积优化

基础优化即可：

- highlight.js 只引入必要样式
- MarkdownRenderer 懒加载或普通加载均可
- 路由组件使用动态 import

路由示例：

```ts
const DashboardPage = () => import('@/modules/dashboard/pages/DashboardPage.vue')
```

## 17. 前端 Smoke Test 文档

新增：

```text
docs/frontend-smoke-test-plan.md
```

覆盖：

- 登录
- 项目创建
- 任务创建/执行
- Chat SSE
- Knowledge 上传/RAG 搜索
- Observability
- Logout

本阶段可以只写文档，不强制 Playwright。

## 18. 可选 Playwright E2E

如果时间允许：

新增：

```text
frontend/playwright.config.ts
frontend/tests/smoke.spec.ts
```

覆盖：

- login
- dashboard visible
- projects visible

不要求覆盖所有页面。

## 19. 验收标准

### 19.1 安装

```bash
cd frontend
npm install
```

### 19.2 类型检查

```bash
npm run typecheck
```

### 19.3 构建

```bash
npm run build
```

### 19.4 启动

```bash
npm run dev
```

### 19.5 手动验证

必须验证：

1. 登录成功
2. Dashboard 加载
3. Project 创建成功
4. Project Detail Tabs 正常
5. Repository Tab 可显示
6. Members Tab 可显示
7. Task 创建成功
8. Task Detail 可打开
9. Task 执行成功
10. Task logs/artifacts/executions/model logs 可查看
11. Chat SSE 正常
12. Chat references 展示正常
13. Knowledge 文档上传成功
14. Chunk Drawer 可打开
15. RAG Search 正常
16. Observability 筛选可用
17. 非 ADMIN 隐藏 Observability
18. Logout 成功

## 20. 已知限制

允许保留：

- Repository clone/pull 依赖后端和本地 Git 环境
- Member 邀请不一定能完成真实邮件通知
- 前端权限只做显示控制
- 暂不做 E2E 全覆盖
- 暂不做移动端优化

## 21. 完成报告模板

完成后请按以下格式输出：

```markdown
# Milestone 13 完成报告

## 1. 新增/修改文件清单

...

## 2. Repository / Members 页面实现

...

## 3. Task Detail / Execution / Model Logs 实现

...

## 4. Chat References 优化

...

## 5. Knowledge 体验优化

...

## 6. Observability 筛选增强

...

## 7. 统一 Loading / Empty / Error 状态

...

## 8. 前端 Smoke Test 文档

...

## 9. npm typecheck / build 结果

...

## 10. 手动验证结果

...

## 11. 已知限制

...

## 12. 是否可以进入 Milestone 14：前后端联调与发布准备

...
```

## 22. Milestone 14 预告

如果 Milestone 13 验证通过，下一阶段建议进入：

```text
Milestone 14: 前后端联调与发布准备
```

建议范围：

- 前后端全链路联调脚本
- 生产构建配置
- Nginx 配置
- Dockerfile
- 前后端 Docker Compose
- CI build workflow
- Release checklist
- 部署文档

