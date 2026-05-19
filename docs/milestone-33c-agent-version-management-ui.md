# Milestone 33C: Agent Version Management UI

## 1. 背景

Milestone 33B 与 33B-Fix 已完成：

- 项目级 Agent 启用弹窗。
- Model Config 下拉选择。
- `modelConfigId` 后端校验。
- 项目 Agent 列表展示 `modelProvider/modelName`。
- Docker / E2E 环境修复。
- 后端 195 个测试通过。
- 前端 typecheck / build 通过。
- E2E 19/19 通过。

当前限制：

- Project Agent 启用弹窗中的 Version 下拉只显示 latest published version。
- Agent 版本只能作为后端数据存在，前端没有版本查看入口。
- 用户无法理解每个 Agent 版本的 systemPrompt / toolPolicy / executionPolicy 差异。

Milestone 33C 的目标是补齐 Agent Version 的查看与选择能力，让 Agent 配置具备可演进基础。

## 2. 总目标

实现 Agent Version 管理 UI 的 P0 能力：

1. 后端提供 Agent version 列表与详情接口。
2. 前端 Agent 列表页增加「版本」入口。
3. 前端可查看某个 Agent 的所有版本。
4. 版本详情可展示 systemPrompt / toolPolicy / executionPolicy。
5. Project Agent 启用弹窗的 Version 下拉改为加载该 Agent 所有 `PUBLISHED` 版本。
6. 后端测试和前端 E2E 覆盖版本查看与选择流程。

## 3. 严格边界

本阶段只做「查看 + 选择」，不做复杂版本编辑后台。

必须遵守：

1. 不实现 Agent Version 创建 / 编辑 / 发布 / 归档。
2. 不新增 Prompt 编辑器。
3. 不新增 toolPolicy 可视化编辑器。
4. 不改 Task Orchestrator 执行逻辑。
5. 不改 Model Gateway 调用逻辑。
6. 不接真实模型 API。
7. 不破坏 33A / 33B / 33B-Fix 已通过测试。
8. 不删除已有 E2E。
9. 前端保持当前中文科技风 UI。
10. 对外 ID 继续使用 String。

允许做：

- 新增 Agent Version 查询 DTO。
- 新增 Agent Version 查询接口。
- 修改 AgentApplicationService 增加查询方法。
- 修改 AgentController 增加 routes。
- 修改 AgentListPage 增加版本入口。
- 新增 AgentVersionDrawer / AgentVersionPage。
- 修改 ProjectAgentConfigPage 的启用弹窗，让 Version 下拉加载完整 published versions。
- 补充测试。

## 4. 后端 API 设计

### 4.1 查询 Agent 版本列表

```http
GET /api/agents/{agentId}/versions
```

权限：

- 登录用户即可访问。

响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "310002",
      "agentId": "300002",
      "versionNo": "v1.0.0",
      "status": "PUBLISHED",
      "systemPrompt": "You are a backend engineering agent...",
      "toolPolicy": "{...}",
      "executionPolicy": "{...}",
      "publishTime": "2026-05-18T10:00:00",
      "createTime": "2026-05-18T10:00:00"
    }
  ]
}
```

排序建议：

```text
create_time DESC 或 version_no DESC
```

### 4.2 查询 Agent 版本详情

```http
GET /api/agents/{agentId}/versions/{versionId}
```

权限：

- 登录用户即可访问。

校验：

- version 必须存在。
- version.agentId 必须等于 path agentId。
- 不匹配时返回 BAD_REQUEST 或 NOT_FOUND。

响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "310002",
    "agentId": "300002",
    "versionNo": "v1.0.0",
    "status": "PUBLISHED",
    "systemPrompt": "...",
    "toolPolicy": "...",
    "executionPolicy": "...",
    "publishTime": "...",
    "createTime": "..."
  }
}
```

## 5. 后端建议修改文件

### 5.1 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/agent/dto/AgentVersionResponse.java
```

字段建议：

```java
private String id;
private String agentId;
private String versionNo;
private String status;
private String systemPrompt;
private String toolPolicy;
private String executionPolicy;
private LocalDateTime publishTime;
private LocalDateTime createTime;
```

按项目规范手写 getter/setter。

### 5.2 Service

修改：

```text
backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java
```

新增方法：

```java
List<AgentVersionResponse> listAgentVersions(Long agentId)
AgentVersionResponse getAgentVersion(Long agentId, Long versionId)
List<AgentVersionResponse> listPublishedAgentVersions(Long agentId)
```

说明：

- `listAgentVersions()` 给 Agent 版本查看 UI 使用。
- `getAgentVersion()` 给详情抽屉使用。
- `listPublishedAgentVersions()` 可复用给 Project Agent 启用弹窗或前端自行过滤。

如果不想额外暴露 published endpoint，前端可以调用 list 然后过滤 `status === 'PUBLISHED'`。

### 5.3 Controller

修改：

```text
backend/src/main/java/com/aicoding/platform/agent/controller/AgentController.java
```

新增：

```java
@GetMapping("/api/agents/{agentId}/versions")
public ApiResponse<List<AgentVersionResponse>> listVersions(@PathVariable String agentId)

@GetMapping("/api/agents/{agentId}/versions/{versionId}")
public ApiResponse<AgentVersionResponse> getVersion(
    @PathVariable String agentId,
    @PathVariable String versionId
)
```

### 5.4 Error Handling

优先复用现有错误码：

- Agent 不存在：`BAD_REQUEST` / `NOT_FOUND`
- Version 不存在：`BAD_REQUEST` / `NOT_FOUND`
- Version 不属于 Agent：`BAD_REQUEST`

如果项目已有更具体 Agent 错误码，优先使用已有风格。

## 6. 前端设计

### 6.1 API 类型

修改：

```text
frontend/src/modules/agent/api.ts
```

新增类型：

```ts
export interface AgentVersion {
  id: string
  agentId: string
  versionNo: string
  status: string
  systemPrompt: string
  toolPolicy: string
  executionPolicy: string
  publishTime: string | null
  createTime: string
}
```

新增方法：

```ts
export function getAgentVersions(agentId: string) {
  return client.get<ApiResponse<AgentVersion[]>>(`/api/agents/${agentId}/versions`)
}

export function getAgentVersion(agentId: string, versionId: string) {
  return client.get<ApiResponse<AgentVersion>>(`/api/agents/${agentId}/versions/${versionId}`)
}
```

### 6.2 Agent List 页面

修改：

```text
frontend/src/modules/agent/pages/AgentListPage.vue
```

新增操作列按钮：

```text
版本
```

点击后打开版本抽屉或跳转版本页。

推荐 P0 用 Drawer：

```text
AgentVersionDrawer.vue
```

好处：

- 不新增路由。
- 不影响现有导航结构。
- 实现成本低。

### 6.3 AgentVersionDrawer

新增：

```text
frontend/src/modules/agent/components/AgentVersionDrawer.vue
```

内容：

- Drawer 标题：`智能体版本`
- 顶部显示 Agent 名称 / Code。
- 左侧或上方版本列表。
- 版本卡片显示：
  - versionNo
  - status
  - publishTime
  - createTime
- 点击版本后显示详情：
  - systemPrompt
  - toolPolicy
  - executionPolicy

视觉建议：

- 继续使用暗色科技风。
- systemPrompt / policy 用 `<pre>` 或 MarkdownRenderer。
- 长文本保持可滚动。

### 6.4 ProjectAgentConfigPage 启用弹窗增强

修改：

```text
frontend/src/modules/agent/pages/ProjectAgentConfigPage.vue
```

当前 Version 下拉只显示当前 latest published version。

改为：

1. 打开启用弹窗时调用 `getAgentVersions(agentId)`。
2. 过滤 `status === 'PUBLISHED'`。
3. 填入 Version 下拉。
4. 默认选中当前行 `agentVersionId`，如果没有则选第一个 PUBLISHED version。
5. 如果没有 PUBLISHED version：
   - 禁用确认按钮。
   - 显示提示：`该智能体暂无已发布版本，无法启用`。

Model Config 下拉保持 33B 行为。

## 7. 后端测试要求

修改或新增：

```text
backend/src/test/java/com/aicoding/platform/agent/AgentVersionIntegrationTest.java
```

或者追加到：

```text
backend/src/test/java/com/aicoding/platform/agent/AgentProjectConfigIntegrationTest.java
```

建议新增独立测试类。

至少覆盖：

1. 登录用户可查询 Agent 版本列表。
2. 未登录访问版本列表返回 UNAUTHORIZED。
3. 版本列表包含 PUBLISHED 版本。
4. 版本详情返回 systemPrompt / toolPolicy / executionPolicy。
5. versionId 不存在返回 BAD_REQUEST / NOT_FOUND。
6. version 不属于 agent 返回 BAD_REQUEST。
7. Project Agent enable 可以选择非 latest 但 PUBLISHED 的版本。
8. Project Agent enable 仍然拒绝 DRAFT / ARCHIVED 版本。

## 8. 前端 E2E 要求

修改或新增：

```text
frontend/e2e/agent-version.spec.ts
frontend/e2e/project-agent-config.spec.ts
```

至少覆盖：

1. 登录。
2. 打开「智能体」页面。
3. 点击某个 Agent 的「版本」按钮。
4. Drawer 打开。
5. 版本列表可见。
6. 点击版本后能看到 systemPrompt / toolPolicy / executionPolicy 区域。
7. 打开项目「智能体」Tab。
8. 点击启用。
9. Version 下拉展示 PUBLISHED 版本。
10. 选择版本并启用成功。

建议 data-testid：

```text
btn-agent-versions
agent-version-drawer
agent-version-list
agent-version-item
agent-version-detail
select-agent-version
```

## 9. 必须执行验证

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果 E2E 需要 Docker 后端，确保镜像包含最新代码：

```bash
docker build -t ai-coding-platform-backend:local ./backend
docker compose -f deploy/docker-compose.app.yml up -d --build
```

## 10. 手动验证清单

访问：

```text
http://localhost:5173
```

验证：

1. 登录成功。
2. 打开「智能体」页面。
3. 点击「版本」。
4. Drawer 打开且样式正常。
5. 能看到版本列表。
6. 能看到 systemPrompt / toolPolicy / executionPolicy。
7. 打开项目详情 → 智能体 Tab。
8. 点击启用。
9. Version 下拉展示多个 PUBLISHED 版本（如果数据存在）。
10. 无 PUBLISHED version 时提示清晰。
11. 选择版本 + 模型配置后启用成功。

## 11. 已知非目标

本阶段不做：

- 创建 Agent Version。
- 编辑 Agent Version。
- 发布 / 归档 Agent Version。
- Prompt Diff。
- Prompt 审批流程。
- Agent Version 回滚。
- Runtime Config Editor。
- 多模型 fallback 策略。

后续可拆：

```text
Milestone 33D: Project Agent Runtime Config Editor
Milestone 33E: Agent Version Create / Publish Workflow
Milestone 34: Multi-model Fallback Strategy UI
```

## 12. 完成报告格式

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. 后端 Agent Version API 说明
3. 前端 Agent Version Drawer 说明
4. Project Agent 启用弹窗版本选择增强说明
5. 后端测试覆盖说明
6. 前端 E2E 覆盖说明
7. 后端 mvn test 结果
8. 前端 typecheck / build / E2E 结果
9. 手动 UI 验证结果
10. 已知限制
11. 是否可以进入 Milestone 33D

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 33C。

文档路径：

```text
docs/milestone-33c-agent-version-management-ui.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段只做 Agent Version 查看与 Project Agent 启用时的版本选择。
3. 不要实现 Agent Version 创建、编辑、发布、归档。
4. 不要新增 Prompt 编辑器。
5. 不要重写 Agent / Project / Model Gateway 架构。
6. 不要改 Task Orchestrator 执行逻辑。
7. 不要破坏 33A / 33B / 33B-Fix 已通过测试。
8. 不要删除已有 E2E。
9. 前端保持当前中文科技风 UI。
10. 对外 ID 继续使用 String。

需要实现：

1. 后端新增 `AgentVersionResponse`。
2. 后端新增 `GET /api/agents/{agentId}/versions`。
3. 后端新增 `GET /api/agents/{agentId}/versions/{versionId}`。
4. 后端校验 version 属于 agent。
5. 前端 `agent/api.ts` 增加 `AgentVersion` 类型和查询函数。
6. 前端 `AgentListPage.vue` 增加「版本」按钮。
7. 前端新增 `AgentVersionDrawer.vue`。
8. Drawer 展示版本列表和 systemPrompt/toolPolicy/executionPolicy。
9. `ProjectAgentConfigPage.vue` 启用弹窗打开时加载该 Agent 所有版本。
10. Version 下拉只展示 `PUBLISHED` 版本。
11. 无 PUBLISHED version 时禁用确认按钮并显示提示。
12. 补充后端测试。
13. 补充前端 E2E。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

完成后按文档第 12 节格式输出报告。

现在开始实现，不要只给计划。
