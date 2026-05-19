# Milestone 33D: Project Agent Runtime Config Editor

## 1. 背景

Milestone 33C 与 33C-Fix 已完成：

- Agent Version 查询 API。
- Agent Version Drawer。
- Project Agent 启用弹窗可选择 PUBLISHED 版本。
- E2E 环境验证码配置已脚本化。
- 后端 205 个测试通过。
- 前端 typecheck / build 通过。
- E2E 23/23 通过。

当前 Project Agent 配置已经具备：

- Agent 启用 / 停用。
- Agent Version 选择。
- Model Config 选择。

但仍缺少运行参数配置：

- temperature
- maxTokens
- timeoutSeconds
- useRag
- knowledgeBaseId
- customInstruction

Milestone 33D 的目标是补齐 Project Agent Runtime Config Editor，让项目内每个 Agent 能有自己的运行参数。

## 2. 总目标

实现项目 Agent 运行配置闭环：

1. 前端启用 / 编辑 Agent 时可配置 runtime config。
2. 后端保存 runtime config 到 `project_agent_config.config`。
3. 后端校验 runtime config 参数范围。
4. Project Agent 列表展示配置摘要。
5. Task / Orchestrator 执行时能读取项目 Agent runtime config。
6. 测试覆盖保存、读取、非法参数、执行链路。

完成后，项目 Agent 将具备：

```text
Agent Definition ✅
Agent Version ✅
Model Config ✅
Runtime Config ✅
```

## 3. 严格边界

本阶段只做单个 Project Agent 的运行配置编辑。

必须遵守：

1. 不做多 Agent 编排。
2. 不做 Agent 自动拆任务。
3. 不做 Prompt 编辑器。
4. 不做 toolPolicy 可视化编辑器。
5. 不改 Agent Version 发布流程。
6. 不新增数据库表。
7. 不接真实模型 API。
8. 不改 Model Gateway Provider 实现。
9. 不破坏 33A / 33B / 33C / 33C-Fix 已通过测试。
10. 前端保持当前中文科技风 UI。

允许做：

- 修改 `EnableProjectAgentRequest` 的 config 结构。
- 新增 runtime config DTO / helper。
- 修改 `AgentApplicationService` 保存和校验 config。
- 修改 `ProjectAgentConfigResponse` 增加 config / configSummary。
- 修改 `ProjectAgentConfigPage.vue` 增加运行配置表单。
- 修改 Orchestrator 读取 project agent config。
- 新增测试。

## 4. Runtime Config 设计

### 4.1 JSON 结构

存储在：

```text
project_agent_config.config
```

建议 JSON：

```json
{
  "temperature": 0.2,
  "maxTokens": 4096,
  "timeoutSeconds": 60,
  "useRag": true,
  "knowledgeBaseId": "2054487957508165634",
  "customInstruction": "优先遵循项目代码规范，输出简洁方案。"
}
```

字段说明：

| 字段 | 类型 | 默认值 | 校验 |
|---|---|---:|---|
| temperature | number | 0.2 | 0.0 - 2.0 |
| maxTokens | number | 4096 | 256 - 32768 |
| timeoutSeconds | number | 60 | 5 - 600 |
| useRag | boolean | false | boolean |
| knowledgeBaseId | string/null | null | useRag=true 时可选，非空需属于项目 |
| customInstruction | string | "" | 最长 2000 字符 |

### 4.2 默认值

如果前端不传 config，后端使用：

```json
{
  "temperature": 0.2,
  "maxTokens": 4096,
  "timeoutSeconds": 60,
  "useRag": false,
  "knowledgeBaseId": null,
  "customInstruction": ""
}
```

### 4.3 配置摘要

后端可返回：

```text
Temp 0.2 / 4096 tokens / RAG on
```

或前端自行根据 config 渲染。

推荐后端返回结构化 config，前端生成展示摘要。

## 5. 后端 API 设计

### 5.1 Enable 项目 Agent

```http
POST /api/projects/{projectId}/agents/{agentId}/enable
```

请求：

```json
{
  "agentVersionId": "310002",
  "modelConfigId": "900001",
  "config": {
    "temperature": 0.2,
    "maxTokens": 4096,
    "timeoutSeconds": 60,
    "useRag": true,
    "knowledgeBaseId": "2054487957508165634",
    "customInstruction": "优先使用项目知识库上下文。"
  }
}
```

行为：

- 保存 `agentVersionId`。
- 保存 `modelConfigId`。
- 校验并保存 `config`。
- enabled=true。

### 5.2 Update 项目 Agent Runtime Config

可选新增接口：

```http
PUT /api/projects/{projectId}/agents/{agentId}/config
```

如果不想新增接口，也可以复用 enable 接口作为 upsert：

```text
enabled=true 时再次 POST enable，相当于更新配置。
```

推荐本阶段复用 enable 接口，减少 API 面。

### 5.3 GET 项目 Agent 列表

```http
GET /api/projects/{projectId}/agents
```

响应增加：

```json
{
  "config": {
    "temperature": 0.2,
    "maxTokens": 4096,
    "timeoutSeconds": 60,
    "useRag": true,
    "knowledgeBaseId": "2054487957508165634",
    "customInstruction": "..."
  }
}
```

如果 config 为空，返回默认 config 或 null 均可。

推荐返回默认 config，前端更简单。

## 6. 后端建议修改文件

### 6.1 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/agent/dto/ProjectAgentRuntimeConfig.java
```

字段：

```java
private BigDecimal temperature;
private Integer maxTokens;
private Integer timeoutSeconds;
private Boolean useRag;
private String knowledgeBaseId;
private String customInstruction;
```

手写 getter/setter。

修改：

```text
backend/src/main/java/com/aicoding/platform/agent/dto/EnableProjectAgentRequest.java
backend/src/main/java/com/aicoding/platform/agent/dto/ProjectAgentConfigResponse.java
```

`EnableProjectAgentRequest`：

```java
private ProjectAgentRuntimeConfig config;
```

`ProjectAgentConfigResponse`：

```java
private ProjectAgentRuntimeConfig config;
```

如果当前 DTO 已使用 Map，可迁移到强类型 DTO，但注意前端类型同步。

### 6.2 Service

修改：

```text
backend/src/main/java/com/aicoding/platform/agent/application/AgentApplicationService.java
```

新增 helper：

```java
ProjectAgentRuntimeConfig normalizeRuntimeConfig(ProjectAgentRuntimeConfig input)
void validateRuntimeConfig(Long projectId, ProjectAgentRuntimeConfig config)
String serializeRuntimeConfig(ProjectAgentRuntimeConfig config)
ProjectAgentRuntimeConfig deserializeRuntimeConfig(String configJson)
```

校验：

- temperature: 0.0 <= x <= 2.0
- maxTokens: 256 <= x <= 32768
- timeoutSeconds: 5 <= x <= 600
- customInstruction length <= 2000
- useRag=true 且 knowledgeBaseId 非空时，校验知识库属于 project

知识库校验可注入：

```text
KnowledgeBaseMapper
```

如果不希望本阶段引入 RAG 依赖，也可先只校验格式，文档说明限制。

推荐：校验 knowledgeBaseId 属于 project，避免跨项目引用。

### 6.3 Orchestrator 读取配置

修改：

```text
backend/src/main/java/com/aicoding/platform/orchestrator/application/AgentOrchestratorService.java
```

目标：

- Task 执行时找到 project agent config。
- 读取 runtime config。
- 如果 `useRag=true`，默认启用 RAG。
- 如果 `knowledgeBaseId` 非空，传给 RAG 查询。
- `customInstruction` 拼入 prompt。
- `temperature/maxTokens/timeoutSeconds` 可先进入 `ModelRequest`，如果当前 ModelRequest 支持。

如果当前 ModelRequest 不支持这些字段：

- 至少将 customInstruction / useRag / knowledgeBaseId 应用到 prompt/RAG。
- temperature/maxTokens 先保存展示，不强制影响真实 provider。

## 7. 前端设计

### 7.1 API 类型

修改：

```text
frontend/src/modules/agent/api.ts
```

新增：

```ts
export interface ProjectAgentRuntimeConfig {
  temperature: number
  maxTokens: number
  timeoutSeconds: number
  useRag: boolean
  knowledgeBaseId: string | null
  customInstruction: string
}
```

`EnableProjectAgentPayload`：

```ts
config?: ProjectAgentRuntimeConfig
```

`ProjectAgentConfig`：

```ts
config: ProjectAgentRuntimeConfig
```

### 7.2 ProjectAgentConfigPage

修改：

```text
frontend/src/modules/agent/pages/ProjectAgentConfigPage.vue
```

启用弹窗增加「运行配置」区域：

表单项：

1. Temperature
   - `el-slider` 或 `el-input-number`
   - min=0 max=2 step=0.1

2. Max Tokens
   - `el-input-number`
   - min=256 max=32768 step=256

3. Timeout Seconds
   - `el-input-number`
   - min=5 max=600 step=5

4. Use RAG
   - `el-switch`

5. Knowledge Base
   - `el-select`
   - 只有 `useRag=true` 时启用
   - 调用知识库 API 获取当前项目知识库

6. Custom Instruction
   - `el-input type="textarea"`
   - max length 2000

交互：

- 点击未启用 Agent 的「启用」：打开弹窗，填默认 runtime config。
- 点击已启用 Agent 的「配置」：打开弹窗，填当前 config。
- 保存后刷新表格。
- 启用和配置可以共用同一个弹窗。

表格新增展示：

```text
运行配置：Temp 0.2 / 4096 tokens / RAG 开
```

### 7.3 Knowledge Base 下拉

复用：

```text
frontend/src/modules/knowledge/api.ts
```

调用当前项目知识库列表：

```ts
listKnowledgeBases(projectId, 1, 100)
```

如果没有知识库：

- 下拉显示空。
- useRag 仍可开启，但 knowledgeBaseId 为空，后端使用默认 RAG 搜索范围。

## 8. 后端测试要求

修改或新增：

```text
backend/src/test/java/com/aicoding/platform/agent/AgentProjectConfigIntegrationTest.java
```

至少覆盖：

1. enable 保存 runtime config 成功。
2. GET 列表返回 runtime config。
3. temperature < 0 或 > 2 返回 BAD_REQUEST。
4. maxTokens 过小 / 过大返回 BAD_REQUEST。
5. timeoutSeconds 过小 / 过大返回 BAD_REQUEST。
6. customInstruction 超长返回 BAD_REQUEST。
7. useRag=true 且 knowledgeBaseId 不属于项目返回 BAD_REQUEST。
8. config 为空时使用默认值。
9. 已启用 Agent 再次 enable 可更新 config。

如果修改 Orchestrator：

```text
backend/src/test/java/com/aicoding/platform/task/TaskOrchestratorIntegrationTest.java
```

补充：

- project agent config 的 customInstruction 会进入 execution inputPrompt。
- useRag=true 时执行日志包含 RAG_SEARCH 或 references。

## 9. 前端 E2E 要求

修改或新增：

```text
frontend/e2e/project-agent-config.spec.ts
```

至少覆盖：

1. 打开项目智能体 Tab。
2. 点击启用。
3. 弹窗显示运行配置区域。
4. 修改 temperature/maxTokens/timeoutSeconds。
5. 开启 useRag。
6. 如果有知识库，选择知识库。
7. 填写 customInstruction。
8. 保存成功。
9. 表格展示运行配置摘要。
10. 已启用 Agent 点击「配置」可回显配置。

建议 data-testid：

```text
agent-runtime-config-section
input-agent-temperature
input-agent-max-tokens
input-agent-timeout
switch-agent-use-rag
select-agent-knowledge-base
input-agent-custom-instruction
agent-runtime-summary
btn-agent-configure
```

## 10. 必须执行验证

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

如果使用 Docker E2E 后端：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 11. 手动验证清单

访问：

```text
http://localhost:5173
```

验证：

1. 登录成功。
2. 打开项目详情。
3. 打开「智能体」Tab。
4. 点击「启用」。
5. 弹窗包含 Version、Model Config、Runtime Config。
6. 修改 temperature/maxTokens/timeoutSeconds。
7. 开启 RAG。
8. 选择知识库。
9. 填写 customInstruction。
10. 保存成功。
11. 表格展示运行配置摘要。
12. 点击「配置」能回显保存值。
13. 任务执行时可以看到 customInstruction 进入 prompt 或日志。

## 12. 已知非目标

本阶段不做：

- Multi-Agent orchestration。
- Agent 自动拆任务。
- Agent Prompt 编辑器。
- Prompt diff。
- Tool policy 可视化编辑。
- 多模型 fallback 策略 UI。
- Runtime config 历史版本。

后续：

```text
Milestone 35A: Multi-Agent Mock Orchestration
Milestone 35B: Agent Execution Plan & Subtasks
Milestone 35C: Review Agent Aggregation
```

## 13. 完成报告格式

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. Runtime Config DTO / JSON 结构说明
3. 后端保存与校验说明
4. Orchestrator 读取配置说明
5. 前端 Runtime Config Editor 说明
6. Knowledge Base 下拉说明
7. 后端测试覆盖说明
8. 前端 E2E 覆盖说明
9. 后端 mvn test 结果
10. 前端 typecheck / build / E2E 结果
11. 手动 UI 验证结果
12. 已知限制
13. 是否可以进入 Milestone 35A

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 33D。

文档路径：

```text
docs/milestone-33d-project-agent-runtime-config-editor.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段只做 Project Agent Runtime Config Editor。
3. 不要实现 Multi-Agent orchestration。
4. 不要实现 Agent 自动拆任务。
5. 不要新增 Prompt 编辑器。
6. 不要新增数据库表。
7. 不要接真实模型 API。
8. 不要破坏 33A / 33B / 33C / 33C-Fix 已通过测试。
9. 前端保持当前中文科技风 UI。
10. Runtime config 存入 `project_agent_config.config`。

需要实现：

1. 新增或补齐 `ProjectAgentRuntimeConfig` DTO。
2. `EnableProjectAgentRequest` 支持强类型 runtime config。
3. `ProjectAgentConfigResponse` 返回 runtime config。
4. 后端校验 temperature/maxTokens/timeoutSeconds/customInstruction/useRag/knowledgeBaseId。
5. config 为空时使用默认值。
6. 已启用 Agent 再次 enable 可更新 runtime config。
7. Orchestrator 读取 project agent runtime config，至少让 customInstruction 进入 prompt。
8. 如果可行，让 useRag / knowledgeBaseId 影响 RAG 查询。
9. 前端启用弹窗增加 Runtime Config 区域。
10. 前端已启用 Agent 增加「配置」按钮，可编辑并回显配置。
11. 前端表格展示运行配置摘要。
12. 前端 Knowledge Base 下拉复用现有 knowledge api。
13. 补充后端测试。
14. 补充前端 E2E。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

完成后按文档第 13 节格式输出报告。

现在开始实现，不要只给计划。
